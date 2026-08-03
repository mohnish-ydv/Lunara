-- Lunara online foundation: profiles, discovery, requests, connections and blocking.
-- Run this entire file in Supabase Dashboard > SQL Editor.

create extension if not exists pgcrypto;
create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to authenticated;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null default '',
    username text unique,
    display_name text not null default '',
    bio text not null default '' check (char_length(bio) <= 120),
    avatar_seed integer not null default 0 check (avatar_seed between 0 and 5),
    share_code text,
    is_discoverable boolean not null default true,
    allow_requests boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint username_format check (
        username is null or username ~ '^[a-z0-9_.]{3,20}$'
    )
);

alter table public.profiles add column if not exists share_code text;
alter table public.profiles add column if not exists is_discoverable boolean not null default true;
alter table public.profiles add column if not exists allow_requests boolean not null default true;

update public.profiles
set share_code = 'LN-' || upper(substr(md5(id::text), 1, 8))
where share_code is null or share_code = '';

alter table public.profiles
    alter column share_code set default ('LN-' || upper(substr(md5(gen_random_uuid()::text), 1, 8)));
alter table public.profiles alter column share_code set not null;

create unique index if not exists profiles_share_code_unique_idx on public.profiles (share_code);
create index if not exists profiles_username_search_idx on public.profiles using btree (username text_pattern_ops);
create index if not exists profiles_display_name_search_idx on public.profiles using btree (display_name text_pattern_ops);

create table if not exists public.connections (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references public.profiles(id) on delete cascade,
    recipient_id uuid not null references public.profiles(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint connection_distinct_people check (requester_id <> recipient_id)
);

create unique index if not exists connections_unique_pair_idx
on public.connections (least(requester_id, recipient_id), greatest(requester_id, recipient_id));
create index if not exists connections_requester_idx on public.connections (requester_id, status);
create index if not exists connections_recipient_idx on public.connections (recipient_id, status);

create table if not exists public.blocks (
    blocker_id uuid not null references public.profiles(id) on delete cascade,
    blocked_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (blocker_id, blocked_id),
    constraint block_distinct_people check (blocker_id <> blocked_id)
);

create or replace function private.is_blocked_between(first_user uuid, second_user uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.blocks
        where (blocker_id = first_user and blocked_id = second_user)
           or (blocker_id = second_user and blocked_id = first_user)
    );
$$;

create or replace function private.has_blocked(viewer_id uuid, profile_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.blocks
        where blocker_id = viewer_id and blocked_id = profile_id
    );
$$;

revoke all on function private.is_blocked_between(uuid, uuid) from public;
revoke all on function private.has_blocked(uuid, uuid) from public;
grant execute on function private.is_blocked_between(uuid, uuid) to authenticated;
grant execute on function private.has_blocked(uuid, uuid) to authenticated;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
before update on public.profiles
for each row execute procedure public.set_updated_at();

drop trigger if exists connections_set_updated_at on public.connections;
create trigger connections_set_updated_at
before update on public.connections
for each row execute procedure public.set_updated_at();

create or replace function public.remove_relationship_on_block()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    delete from public.connections
    where (requester_id = new.blocker_id and recipient_id = new.blocked_id)
       or (requester_id = new.blocked_id and recipient_id = new.blocker_id);
    return new;
end;
$$;

drop trigger if exists blocks_remove_relationship on public.blocks;
create trigger blocks_remove_relationship
after insert on public.blocks
for each row execute procedure public.remove_relationship_on_block();

alter table public.profiles enable row level security;
alter table public.connections enable row level security;
alter table public.blocks enable row level security;

drop policy if exists "Authenticated users can discover profiles" on public.profiles;
drop policy if exists "Profiles visible to allowed people" on public.profiles;
create policy "Profiles visible to allowed people"
on public.profiles for select
to authenticated
using (
    (select auth.uid()) = id
    or private.has_blocked((select auth.uid()), profiles.id)
    or (
        not private.is_blocked_between((select auth.uid()), profiles.id)
        and (
            is_discoverable
            or exists (
                select 1 from public.connections c
                where ((c.requester_id = (select auth.uid()) and c.recipient_id = profiles.id)
                    or (c.recipient_id = (select auth.uid()) and c.requester_id = profiles.id))
            )
        )
    )
);

drop policy if exists "Users can create their own profile" on public.profiles;
create policy "Users can create their own profile"
on public.profiles for insert
to authenticated
with check ((select auth.uid()) = id);

drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
on public.profiles for update
to authenticated
using ((select auth.uid()) = id)
with check ((select auth.uid()) = id);

drop policy if exists "Users can delete their own profile" on public.profiles;
create policy "Users can delete their own profile"
on public.profiles for delete
to authenticated
using ((select auth.uid()) = id);

drop policy if exists "Participants can read connections" on public.connections;
create policy "Participants can read connections"
on public.connections for select
to authenticated
using ((select auth.uid()) in (requester_id, recipient_id));

drop policy if exists "Users can send valid requests" on public.connections;
create policy "Users can send valid requests"
on public.connections for insert
to authenticated
with check (
    (select auth.uid()) = requester_id
    and status = 'pending'
    and exists (
        select 1 from public.profiles recipient
        where recipient.id = recipient_id and recipient.allow_requests
    )
    and not private.is_blocked_between(requester_id, recipient_id)
);

drop policy if exists "Recipients can accept requests" on public.connections;
create policy "Recipients can accept requests"
on public.connections for update
to authenticated
using ((select auth.uid()) = recipient_id and status = 'pending')
with check ((select auth.uid()) = recipient_id and status = 'accepted');

drop policy if exists "Participants can remove connections" on public.connections;
create policy "Participants can remove connections"
on public.connections for delete
to authenticated
using ((select auth.uid()) in (requester_id, recipient_id));

drop policy if exists "Users can read their blocked list" on public.blocks;
create policy "Users can read their blocked list"
on public.blocks for select
to authenticated
using ((select auth.uid()) = blocker_id);

drop policy if exists "Users can block profiles" on public.blocks;
create policy "Users can block profiles"
on public.blocks for insert
to authenticated
with check ((select auth.uid()) = blocker_id);

drop policy if exists "Users can unblock profiles" on public.blocks;
create policy "Users can unblock profiles"
on public.blocks for delete
to authenticated
using ((select auth.uid()) = blocker_id);

-- M3: direct conversations, realtime messages, receipts, reactions, typing and presence.

create table if not exists public.conversations (
    id uuid primary key default gen_random_uuid(),
    participant_a uuid not null references public.profiles(id) on delete cascade,
    participant_b uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint conversations_distinct_people check (participant_a <> participant_b),
    constraint conversations_ordered_people check (participant_a < participant_b),
    constraint conversations_unique_people unique (participant_a, participant_b)
);

create index if not exists conversations_a_updated_idx on public.conversations (participant_a, updated_at desc);
create index if not exists conversations_b_updated_idx on public.conversations (participant_b, updated_at desc);

create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    client_id uuid not null,
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    sender_id uuid not null references public.profiles(id) on delete cascade,
    body text not null default '' check (char_length(body) <= 4000),
    reply_to_id uuid references public.messages(id) on delete set null,
    created_at timestamptz not null default now(),
    edited_at timestamptz,
    deleted_at timestamptz,
    constraint messages_sender_client_unique unique (sender_id, client_id)
);

create index if not exists messages_conversation_time_idx on public.messages (conversation_id, created_at desc);
create index if not exists messages_reply_idx on public.messages (reply_to_id);

-- M5: structured live message cards. Text remains the searchable, accessible summary.
alter table public.messages add column if not exists module_type text not null default 'text';
alter table public.messages add column if not exists module_payload jsonb not null default '{}'::jsonb;
alter table public.messages add column if not exists module_revision integer not null default 0;

-- M6: private media metadata. Binary objects live in the private chat-media bucket.
alter table public.messages add column if not exists media_type text not null default 'none';
alter table public.messages add column if not exists media_payload jsonb not null default '{}'::jsonb;

create or replace function private.valid_message_media(candidate_type text, candidate_payload jsonb)
returns boolean
language plpgsql
immutable
set search_path = ''
as $$
declare
    sample jsonb;
    object_path text;
begin
    if candidate_type not in ('none', 'image', 'document', 'voice')
       or jsonb_typeof(candidate_payload) <> 'object'
       or octet_length(candidate_payload::text) > 12000 then
        return false;
    end if;
    if candidate_type = 'none' then
        return candidate_payload = '{}'::jsonb;
    end if;
    object_path := btrim(coalesce(candidate_payload->>'remote_path', ''));
    if char_length(btrim(coalesce(candidate_payload->>'id', ''))) not between 1 and 80
       or char_length(btrim(coalesce(candidate_payload->>'file_name', ''))) not between 1 and 180
       or char_length(btrim(coalesce(candidate_payload->>'mime_type', ''))) not between 1 and 120
       or char_length(coalesce(candidate_payload->>'caption', '')) > 1000
       or char_length(object_path) not between 10 and 500
       or coalesce((candidate_payload->>'size_bytes')::bigint, 0) not between 1 and 26214400
       or coalesce((candidate_payload->>'width')::integer, 0) < 0
       or coalesce((candidate_payload->>'height')::integer, 0) < 0
       or coalesce((candidate_payload->>'duration_ms')::bigint, 0) < 0 then
        return false;
    end if;
    if candidate_type = 'image' and coalesce((candidate_payload->>'size_bytes')::bigint, 0) > 12582912 then
        return false;
    end if;
    if candidate_type = 'voice' then
        if coalesce((candidate_payload->>'duration_ms')::bigint, 0) not between 250 and 1200000
           or coalesce((candidate_payload->>'size_bytes')::bigint, 0) > 20971520
           or jsonb_typeof(candidate_payload->'waveform') <> 'array'
           or jsonb_array_length(candidate_payload->'waveform') > 180 then
            return false;
        end if;
        for sample in select value from jsonb_array_elements(candidate_payload->'waveform') loop
            if jsonb_typeof(sample) <> 'number' or (sample::text)::integer not between 0 and 100 then
                return false;
            end if;
        end loop;
    end if;
    return true;
exception when others then
    return false;
end;
$$;

revoke all on function private.valid_message_media(text, jsonb) from public;
grant execute on function private.valid_message_media(text, jsonb) to authenticated;

alter table public.messages drop constraint if exists messages_valid_media;
alter table public.messages add constraint messages_valid_media check (
    private.valid_message_media(media_type, media_payload)
) not valid;
alter table public.messages validate constraint messages_valid_media;

create or replace function private.valid_message_module(candidate_type text, candidate_payload jsonb)
returns boolean
language plpgsql
immutable
set search_path = ''
as $$
declare
    item jsonb;
    option_item jsonb;
    latitude_kind text;
    longitude_kind text;
begin
    if candidate_type not in ('text', 'task', 'checklist', 'poll', 'event', 'reminder', 'note', 'countdown', 'code', 'location', 'contact')
       or jsonb_typeof(candidate_payload) <> 'object'
       or octet_length(candidate_payload::text) > 20000 then
        return false;
    end if;

    if candidate_type = 'text' then
        return true;
    end if;

    if char_length(btrim(coalesce(candidate_payload->>'title', ''))) not between 1 and 120
       or char_length(coalesce(candidate_payload->>'description', '')) > 1200 then
        return false;
    end if;

    if candidate_payload ? 'completed' and jsonb_typeof(candidate_payload->'completed') <> 'boolean' then
        return false;
    end if;

    if candidate_type = 'checklist' then
        if jsonb_typeof(candidate_payload->'items') <> 'array'
           or jsonb_array_length(candidate_payload->'items') not between 2 and 20 then
            return false;
        end if;
        for item in select value from jsonb_array_elements(candidate_payload->'items') loop
            if jsonb_typeof(item) <> 'object'
               or char_length(btrim(coalesce(item->>'id', ''))) not between 1 and 120
               or char_length(btrim(coalesce(item->>'text', ''))) not between 1 and 120
               or (item ? 'completed' and jsonb_typeof(item->'completed') <> 'boolean') then
                return false;
            end if;
        end loop;
        if (select count(*) from jsonb_array_elements(candidate_payload->'items')) <>
           (select count(distinct value->>'id') from jsonb_array_elements(candidate_payload->'items')) then
            return false;
        end if;
    end if;

    if candidate_type = 'poll' then
        if jsonb_typeof(candidate_payload->'options') <> 'array'
           or jsonb_array_length(candidate_payload->'options') not between 2 and 10 then
            return false;
        end if;
        for option_item in select value from jsonb_array_elements(candidate_payload->'options') loop
            if jsonb_typeof(option_item) <> 'object'
               or char_length(btrim(coalesce(option_item->>'id', ''))) not between 1 and 120
               or char_length(btrim(coalesce(option_item->>'text', ''))) not between 1 and 120
               or jsonb_typeof(coalesce(option_item->'voter_ids', '[]'::jsonb)) <> 'array'
               or exists (
                   select 1 from jsonb_array_elements(coalesce(option_item->'voter_ids', '[]'::jsonb)) voter_source(voter)
                   where jsonb_typeof(voter) <> 'string' or btrim(voter #>> '{}') = ''
               )
               or (select count(*) from jsonb_array_elements(coalesce(option_item->'voter_ids', '[]'::jsonb))) <>
                  (select count(distinct voter #>> '{}')
                     from jsonb_array_elements(coalesce(option_item->'voter_ids', '[]'::jsonb)) voter_source(voter)) then
                return false;
            end if;
        end loop;
        if (select count(*) from jsonb_array_elements(candidate_payload->'options')) <>
           (select count(distinct value->>'id') from jsonb_array_elements(candidate_payload->'options')) then
            return false;
        end if;
    end if;

    if candidate_type = 'event' then
        if btrim(coalesce(candidate_payload->>'event_at', '')) = ''
           or jsonb_typeof(coalesce(candidate_payload->'rsvps', '{}'::jsonb)) <> 'object'
           or exists (
               select 1 from jsonb_each_text(coalesce(candidate_payload->'rsvps', '{}'::jsonb))
               where value not in ('going', 'maybe', 'not_going')
           ) then
            return false;
        end if;
    end if;

    if candidate_type in ('reminder', 'countdown')
       and btrim(coalesce(candidate_payload->>'due_at', '')) = '' then
        return false;
    end if;

    if candidate_type = 'code'
       and (btrim(coalesce(candidate_payload->>'code', '')) = ''
            or char_length(coalesce(candidate_payload->>'code', '')) > 8000
            or char_length(coalesce(candidate_payload->>'language', '')) > 32) then
        return false;
    end if;

    if candidate_type = 'location' then
        if btrim(coalesce(candidate_payload->>'location_name', '')) = '' then
            return false;
        end if;
        latitude_kind := coalesce(jsonb_typeof(candidate_payload->'latitude'), 'null');
        longitude_kind := coalesce(jsonb_typeof(candidate_payload->'longitude'), 'null');
        if latitude_kind not in ('number', 'null')
           or longitude_kind not in ('number', 'null')
           or (latitude_kind = 'null') <> (longitude_kind = 'null') then
            return false;
        end if;
        if latitude_kind = 'number' and (
            (candidate_payload->>'latitude')::numeric not between -90 and 90
            or (candidate_payload->>'longitude')::numeric not between -180 and 180
        ) then
            return false;
        end if;
    end if;

    if candidate_type = 'contact'
       and (btrim(coalesce(candidate_payload->>'contact_name', '')) = ''
            or btrim(coalesce(candidate_payload->>'contact_value', '')) = ''
            or char_length(coalesce(candidate_payload->>'contact_name', '')) > 80
            or char_length(coalesce(candidate_payload->>'contact_value', '')) > 180) then
        return false;
    end if;

    return true;
exception when others then
    return false;
end;
$$;

revoke all on function private.valid_message_module(text, jsonb) from public;
grant execute on function private.valid_message_module(text, jsonb) to authenticated;

create or replace function private.valid_message_module_transition(
    candidate_type text,
    previous_payload jsonb,
    next_payload jsonb,
    actor_id uuid
)
returns boolean
language plpgsql
immutable
set search_path = ''
as $$
declare
    previous_shape jsonb;
    next_shape jsonb;
    previous_other_votes jsonb;
    next_other_votes jsonb;
    actor_vote_count integer;
begin
    if actor_id is null or not private.valid_message_module(candidate_type, next_payload) then
        return false;
    end if;

    if candidate_type = 'task' then
        return (previous_payload - 'completed') = (next_payload - 'completed');
    end if;

    if candidate_type = 'checklist' then
        if (previous_payload - 'items') <> (next_payload - 'items') then
            return false;
        end if;
        select jsonb_agg(value - 'completed' order by ordinality)
          into previous_shape
          from jsonb_array_elements(previous_payload->'items') with ordinality;
        select jsonb_agg(value - 'completed' order by ordinality)
          into next_shape
          from jsonb_array_elements(next_payload->'items') with ordinality;
        return previous_shape = next_shape;
    end if;

    if candidate_type = 'poll' then
        if (previous_payload - 'options') <> (next_payload - 'options') then
            return false;
        end if;
        select jsonb_agg(value - 'voter_ids' order by ordinality)
          into previous_shape
          from jsonb_array_elements(previous_payload->'options') with ordinality;
        select jsonb_agg(value - 'voter_ids' order by ordinality)
          into next_shape
          from jsonb_array_elements(next_payload->'options') with ordinality;
        if previous_shape <> next_shape then
            return false;
        end if;

        select jsonb_agg(
                   jsonb_build_object(
                       'id', option_item->>'id',
                       'voters', coalesce((
                           select jsonb_agg(voter #>> '{}' order by voter #>> '{}')
                           from jsonb_array_elements(coalesce(option_item->'voter_ids', '[]'::jsonb)) voter_source(voter)
                           where voter #>> '{}' <> actor_id::text
                       ), '[]'::jsonb)
                   ) order by option_order
               )
          into previous_other_votes
          from jsonb_array_elements(previous_payload->'options') with ordinality source(option_item, option_order);

        select jsonb_agg(
                   jsonb_build_object(
                       'id', option_item->>'id',
                       'voters', coalesce((
                           select jsonb_agg(voter #>> '{}' order by voter #>> '{}')
                           from jsonb_array_elements(coalesce(option_item->'voter_ids', '[]'::jsonb)) voter_source(voter)
                           where voter #>> '{}' <> actor_id::text
                       ), '[]'::jsonb)
                   ) order by option_order
               )
          into next_other_votes
          from jsonb_array_elements(next_payload->'options') with ordinality source(option_item, option_order);

        select count(*)
          into actor_vote_count
          from jsonb_array_elements(next_payload->'options') option_source(option_item),
               jsonb_array_elements(coalesce(option_item->'voter_ids', '[]'::jsonb)) voter_source(voter)
         where voter #>> '{}' = actor_id::text;

        return previous_other_votes = next_other_votes and actor_vote_count <= 1;
    end if;

    if candidate_type = 'event' then
        return (previous_payload - 'rsvps') = (next_payload - 'rsvps')
           and (coalesce(previous_payload->'rsvps', '{}'::jsonb) - actor_id::text) =
               (coalesce(next_payload->'rsvps', '{}'::jsonb) - actor_id::text);
    end if;

    return false;
exception when others then
    return false;
end;
$$;

revoke all on function private.valid_message_module_transition(text, jsonb, jsonb, uuid) from public;

alter table public.messages drop constraint if exists messages_valid_module;
alter table public.messages add constraint messages_valid_module check (
    module_revision >= 0
    and private.valid_message_module(module_type, module_payload)
) not valid;
alter table public.messages validate constraint messages_valid_module;

alter table public.messages drop constraint if exists messages_valid_body;
alter table public.messages add constraint messages_valid_body check (
    (deleted_at is not null and body = '')
    or (deleted_at is null and char_length(btrim(body)) between 1 and 4000)
) not valid;
alter table public.messages validate constraint messages_valid_body;

create table if not exists public.message_receipts (
    message_id uuid not null references public.messages(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    delivered_at timestamptz,
    read_at timestamptz,
    primary key (message_id, user_id)
);

create index if not exists message_receipts_user_idx on public.message_receipts (user_id, read_at);

create table if not exists public.message_reactions (
    message_id uuid not null references public.messages(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    emoji text not null check (char_length(emoji) between 1 and 8),
    created_at timestamptz not null default now(),
    primary key (message_id, user_id)
);

create table if not exists public.typing_states (
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    is_typing boolean not null default false,
    updated_at timestamptz not null default now(),
    primary key (conversation_id, user_id)
);

create table if not exists public.presence_states (
    user_id uuid primary key references public.profiles(id) on delete cascade,
    is_online boolean not null default false,
    last_active_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);


-- M4: personal conversation organization and saved messages.

create or replace function private.valid_conversation_labels(candidate text[])
returns boolean
language sql
immutable
set search_path = ''
as $$
    select cardinality(candidate) <= 4
       and coalesce(bool_and(char_length(btrim(label)) between 1 and 24), true)
    from unnest(candidate) label;
$$;

revoke all on function private.valid_conversation_labels(text[]) from public;
grant execute on function private.valid_conversation_labels(text[]) to authenticated;

create table if not exists public.conversation_preferences (
    user_id uuid not null references public.profiles(id) on delete cascade,
    conversation_id uuid not null references public.conversations(id) on delete cascade,
    is_pinned boolean not null default false,
    is_archived boolean not null default false,
    is_muted boolean not null default false,
    labels text[] not null default '{}'::text[],
    updated_at timestamptz not null default now(),
    primary key (user_id, conversation_id),
    constraint conversation_preferences_labels_valid check (private.valid_conversation_labels(labels))
);

create index if not exists conversation_preferences_user_idx
    on public.conversation_preferences (user_id, is_archived, is_pinned desc, updated_at desc);

drop trigger if exists conversation_preferences_set_updated_at on public.conversation_preferences;
create trigger conversation_preferences_set_updated_at
before update on public.conversation_preferences
for each row execute procedure public.set_updated_at();

create table if not exists public.message_bookmarks (
    user_id uuid not null references public.profiles(id) on delete cascade,
    message_id uuid not null references public.messages(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, message_id)
);

create index if not exists message_bookmarks_user_time_idx
    on public.message_bookmarks (user_id, created_at desc);

create or replace function private.is_conversation_participant(target_conversation uuid, target_user uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.conversations c
        where c.id = target_conversation
          and target_user in (c.participant_a, c.participant_b)
          and not private.is_blocked_between(c.participant_a, c.participant_b)
          and exists (
              select 1 from public.connections relationship
              where relationship.status = 'accepted'
                and ((relationship.requester_id = c.participant_a and relationship.recipient_id = c.participant_b)
                  or (relationship.requester_id = c.participant_b and relationship.recipient_id = c.participant_a))
          )
    );
$$;

create or replace function private.are_connected(first_user uuid, second_user uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select not private.is_blocked_between(first_user, second_user)
       and exists (
            select 1 from public.connections
            where status = 'accepted'
              and ((requester_id = first_user and recipient_id = second_user)
                or (requester_id = second_user and recipient_id = first_user))
       );
$$;

create or replace function private.is_reply_in_conversation(target_message uuid, target_conversation uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.messages
        where id = target_message and conversation_id = target_conversation
    );
$$;

revoke all on function private.is_conversation_participant(uuid, uuid) from public;
revoke all on function private.are_connected(uuid, uuid) from public;
revoke all on function private.is_reply_in_conversation(uuid, uuid) from public;
grant execute on function private.is_conversation_participant(uuid, uuid) to authenticated;
grant execute on function private.are_connected(uuid, uuid) to authenticated;
grant execute on function private.is_reply_in_conversation(uuid, uuid) to authenticated;

create or replace function public.ensure_direct_conversation(other_user uuid)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    current_user_id uuid := auth.uid();
    first_user uuid;
    second_user uuid;
    conversation_id uuid;
begin
    if current_user_id is null or other_user is null or current_user_id = other_user then
        raise exception 'Invalid conversation participants';
    end if;
    if not private.are_connected(current_user_id, other_user) then
        raise exception 'Only connected profiles can start a conversation';
    end if;
    if private.is_blocked_between(current_user_id, other_user) then
        raise exception 'This conversation is unavailable';
    end if;

    first_user := least(current_user_id, other_user);
    second_user := greatest(current_user_id, other_user);

    select id into conversation_id
    from public.conversations
    where participant_a = first_user and participant_b = second_user;

    if conversation_id is null then
        insert into public.conversations (participant_a, participant_b)
        values (first_user, second_user)
        on conflict (participant_a, participant_b)
        do update set updated_at = public.conversations.updated_at
        returning id into conversation_id;
    end if;

    return conversation_id;
end;
$$;

drop function if exists public.list_my_conversations();
create or replace function public.list_my_conversations()
returns table (
    conversation_id uuid,
    person_id uuid,
    person_username text,
    person_display_name text,
    person_bio text,
    person_avatar_seed integer,
    person_share_code text,
    last_message text,
    last_message_at timestamptz,
    unread_count bigint,
    is_online boolean,
    last_active_at timestamptz,
    is_typing boolean,
    is_pinned boolean,
    is_archived boolean,
    is_muted boolean,
    labels jsonb
)
language sql
stable
security invoker
set search_path = ''
as $$
    select
        c.id,
        other_profile.id,
        other_profile.username,
        other_profile.display_name,
        other_profile.bio,
        other_profile.avatar_seed,
        other_profile.share_code,
        case when latest.deleted_at is not null then 'Message removed' else coalesce(latest.body, '') end,
        latest.created_at,
        (
            select count(*)
            from public.messages unread_message
            left join public.message_receipts unread_receipt
              on unread_receipt.message_id = unread_message.id
             and unread_receipt.user_id = auth.uid()
            where unread_message.conversation_id = c.id
              and unread_message.sender_id <> auth.uid()
              and unread_receipt.read_at is null
        ),
        coalesce(presence.is_online and presence.updated_at > now() - interval '90 seconds', false),
        presence.last_active_at,
        coalesce(typing.is_typing and typing.updated_at > now() - interval '10 seconds', false),
        coalesce(preference.is_pinned, false),
        coalesce(preference.is_archived, false),
        coalesce(preference.is_muted, false),
        to_jsonb(coalesce(preference.labels, '{}'::text[]))
    from public.conversations c
    join public.profiles other_profile
      on other_profile.id = case when c.participant_a = auth.uid() then c.participant_b else c.participant_a end
    left join lateral (
        select m.body, m.created_at, m.deleted_at
        from public.messages m
        where m.conversation_id = c.id
        order by m.created_at desc
        limit 1
    ) latest on true
    left join public.presence_states presence on presence.user_id = other_profile.id
    left join public.typing_states typing on typing.conversation_id = c.id and typing.user_id = other_profile.id
    left join public.conversation_preferences preference
      on preference.conversation_id = c.id and preference.user_id = auth.uid()
    where private.is_conversation_participant(c.id, auth.uid())
    order by coalesce(preference.is_pinned, false) desc, coalesce(latest.created_at, c.updated_at) desc;
$$;

drop function if exists public.get_conversation_messages(uuid, timestamptz, integer);
create or replace function public.get_conversation_messages(
    target_conversation uuid,
    before_time timestamptz default null,
    page_size integer default 40
)
returns table (
    id uuid,
    client_id uuid,
    conversation_id uuid,
    sender_id uuid,
    body text,
    created_at timestamptz,
    edited_at timestamptz,
    deleted_at timestamptz,
    reply_to_id uuid,
    reply_preview text,
    delivery_state text,
    reactions jsonb,
    is_bookmarked boolean,
    module_type text,
    module_payload jsonb,
    module_revision integer,
    media_type text,
    media_payload jsonb
)
language sql
stable
security invoker
set search_path = ''
as $$
    select * from (
        select
            m.id,
            m.client_id,
            m.conversation_id,
            m.sender_id,
            m.body,
            m.created_at,
            m.edited_at,
            m.deleted_at,
            m.reply_to_id,
            coalesce(reply_message.body, ''),
            case
                when m.sender_id <> auth.uid() then 'Delivered'
                when receipt.read_at is not null then 'Read'
                when receipt.delivered_at is not null then 'Delivered'
                else 'Sent'
            end,
            coalesce((
                select jsonb_agg(
                    jsonb_build_object(
                        'emoji', grouped.emoji,
                        'count', grouped.reaction_count,
                        'reacted_by_me', grouped.reacted_by_me
                    ) order by grouped.reaction_count desc
                )
                from (
                    select
                        reaction.emoji,
                        count(*) as reaction_count,
                        bool_or(reaction.user_id = auth.uid()) as reacted_by_me
                    from public.message_reactions reaction
                    where reaction.message_id = m.id
                    group by reaction.emoji
                ) grouped
            ), '[]'::jsonb),
            exists (
                select 1 from public.message_bookmarks bookmark
                where bookmark.user_id = auth.uid() and bookmark.message_id = m.id
            ),
            m.module_type,
            m.module_payload,
            m.module_revision,
            m.media_type,
            m.media_payload
        from public.messages m
        left join public.messages reply_message on reply_message.id = m.reply_to_id
        left join public.message_receipts receipt
          on receipt.message_id = m.id
         and receipt.user_id <> m.sender_id
        where m.conversation_id = target_conversation
          and private.is_conversation_participant(target_conversation, auth.uid())
          and (before_time is null or m.created_at < before_time)
        order by m.created_at desc
        limit greatest(1, least(page_size, 500))
    ) page
    order by created_at asc;
$$;

drop function if exists public.search_conversation_messages(uuid, text, integer);
create or replace function public.search_conversation_messages(
    target_conversation uuid,
    search_query text,
    page_size integer default 80
)
returns table (
    id uuid, client_id uuid, conversation_id uuid, sender_id uuid, body text,
    created_at timestamptz, edited_at timestamptz, deleted_at timestamptz,
    reply_to_id uuid, reply_preview text, delivery_state text, reactions jsonb, is_bookmarked boolean,
    module_type text, module_payload jsonb, module_revision integer,
    media_type text, media_payload jsonb
)
language sql
stable
security invoker
set search_path = ''
as $$
    select message_page.*
    from public.get_conversation_messages(target_conversation, null, 500) message_page
    where char_length(btrim(search_query)) > 0
      and message_page.deleted_at is null
      and (message_page.body || ' ' || message_page.module_payload::text || ' ' || message_page.media_payload::text) ilike '%' || replace(replace(btrim(search_query), '%', '\%'), '_', '\_') || '%' escape '\'
    order by message_page.created_at desc
    limit greatest(1, least(page_size, 120));
$$;

drop function if exists public.list_conversation_bookmarks(uuid, integer);
create or replace function public.list_conversation_bookmarks(
    target_conversation uuid,
    page_size integer default 80
)
returns table (
    id uuid, client_id uuid, conversation_id uuid, sender_id uuid, body text,
    created_at timestamptz, edited_at timestamptz, deleted_at timestamptz,
    reply_to_id uuid, reply_preview text, delivery_state text, reactions jsonb, is_bookmarked boolean,
    module_type text, module_payload jsonb, module_revision integer,
    media_type text, media_payload jsonb
)
language sql
stable
security invoker
set search_path = ''
as $$
    select message_page.*
    from public.get_conversation_messages(target_conversation, null, 500) message_page
    where message_page.is_bookmarked
    order by message_page.created_at desc
    limit greatest(1, least(page_size, 120));
$$;

drop function if exists public.list_conversation_links(uuid, integer);
create or replace function public.list_conversation_links(
    target_conversation uuid,
    page_size integer default 80
)
returns table (
    id uuid, client_id uuid, conversation_id uuid, sender_id uuid, body text,
    created_at timestamptz, edited_at timestamptz, deleted_at timestamptz,
    reply_to_id uuid, reply_preview text, delivery_state text, reactions jsonb, is_bookmarked boolean,
    module_type text, module_payload jsonb, module_revision integer,
    media_type text, media_payload jsonb
)
language sql
stable
security invoker
set search_path = ''
as $$
    select message_page.*
    from public.get_conversation_messages(target_conversation, null, 500) message_page
    where message_page.deleted_at is null
      and (message_page.body || ' ' || message_page.module_payload::text || ' ' || message_page.media_payload::text) ~* '(https?://|www\.)[^[:space:]\"}]+'
    order by message_page.created_at desc
    limit greatest(1, least(page_size, 120));
$$;

drop function if exists public.list_conversation_media(uuid, integer);
create or replace function public.list_conversation_media(
    target_conversation uuid,
    page_size integer default 120
)
returns table (
    id uuid, client_id uuid, conversation_id uuid, sender_id uuid, body text,
    created_at timestamptz, edited_at timestamptz, deleted_at timestamptz,
    reply_to_id uuid, reply_preview text, delivery_state text, reactions jsonb, is_bookmarked boolean,
    module_type text, module_payload jsonb, module_revision integer,
    media_type text, media_payload jsonb
)
language sql
stable
security invoker
set search_path = ''
as $$
    select message_page.*
    from public.get_conversation_messages(target_conversation, null, 500) message_page
    where message_page.deleted_at is null
      and message_page.media_type <> 'none'
    order by message_page.created_at desc
    limit greatest(1, least(page_size, 200));
$$;

drop function if exists public.update_message_module(uuid, integer, jsonb);
create or replace function public.update_message_module(
    target_message uuid,
    expected_revision integer,
    next_payload jsonb
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    target public.messages%rowtype;
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    select * into target from public.messages where id = target_message for update;
    if not found then
        raise exception 'Interactive card is no longer available';
    end if;
    if target.deleted_at is not null or target.module_type = 'text' then
        raise exception 'This message is not interactive';
    end if;
    if not private.is_conversation_participant(target.conversation_id, auth.uid()) then
        raise exception 'Conversation access denied';
    end if;
    if target.module_revision <> expected_revision then
        raise exception 'This card changed on another device. Refresh and try again.';
    end if;
    if not private.valid_message_module(target.module_type, next_payload) then
        raise exception 'Interactive card data is invalid';
    end if;
    if not private.valid_message_module_transition(target.module_type, target.module_payload, next_payload, auth.uid()) then
        raise exception 'This card change is not allowed';
    end if;

    update public.messages
       set module_payload = next_payload,
           module_revision = module_revision + 1,
           edited_at = now()
     where id = target_message;
end;
$$;

revoke all on function public.update_message_module(uuid, integer, jsonb) from public;
grant execute on function public.update_message_module(uuid, integer, jsonb) to authenticated;

create or replace function public.mark_conversation_delivered(target_conversation uuid)
returns void
language sql
security invoker
set search_path = ''
as $$
    insert into public.message_receipts (message_id, user_id, delivered_at)
    select m.id, auth.uid(), now()
    from public.messages m
    where m.conversation_id = target_conversation
      and m.sender_id <> auth.uid()
      and private.is_conversation_participant(target_conversation, auth.uid())
    on conflict (message_id, user_id)
    do update set delivered_at = excluded.delivered_at
    where public.message_receipts.delivered_at is null;
$$;

create or replace function public.mark_conversation_read(target_conversation uuid)
returns void
language sql
security invoker
set search_path = ''
as $$
    insert into public.message_receipts (message_id, user_id, delivered_at, read_at)
    select m.id, auth.uid(), now(), now()
    from public.messages m
    where m.conversation_id = target_conversation
      and m.sender_id <> auth.uid()
      and private.is_conversation_participant(target_conversation, auth.uid())
    on conflict (message_id, user_id)
    do update set
        delivered_at = coalesce(public.message_receipts.delivered_at, excluded.delivered_at),
        read_at = excluded.read_at
    where public.message_receipts.read_at is null;
$$;

create or replace function public.set_conversation_typing(target_conversation uuid, typing boolean)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
begin
    if not private.is_conversation_participant(target_conversation, auth.uid()) then
        raise exception 'Conversation unavailable';
    end if;
    insert into public.typing_states (conversation_id, user_id, is_typing, updated_at)
    values (target_conversation, auth.uid(), typing, now())
    on conflict (conversation_id, user_id)
    do update set is_typing = excluded.is_typing, updated_at = excluded.updated_at;
end;
$$;

create or replace function public.set_my_presence(online boolean)
returns void
language sql
security invoker
set search_path = ''
as $$
    insert into public.presence_states (user_id, is_online, last_active_at, updated_at)
    values (auth.uid(), online, now(), now())
    on conflict (user_id)
    do update set is_online = excluded.is_online, last_active_at = now(), updated_at = now();
$$;

create or replace function public.prepare_message_insert()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.created_at := now();
    new.edited_at := null;
    new.deleted_at := null;
    new.module_type := coalesce(nullif(new.module_type, ''), 'text');
    new.module_payload := coalesce(new.module_payload, '{}'::jsonb);
    new.module_revision := 0;
    new.media_type := coalesce(nullif(new.media_type, ''), 'none');
    new.media_payload := coalesce(new.media_payload, '{}'::jsonb);
    if btrim(new.body) = '' then
        raise exception 'Message text cannot be empty';
    end if;
    if not private.valid_message_module(new.module_type, new.module_payload) then
        raise exception 'Interactive card data is invalid';
    end if;
    if not private.valid_message_media(new.media_type, new.media_payload) then
        raise exception 'Media data is invalid';
    end if;
    if new.media_type <> 'none' then
        if split_part(new.media_payload->>'remote_path', '/', 1) <> new.conversation_id::text
           or split_part(new.media_payload->>'remote_path', '/', 2) <> new.sender_id::text
           or split_part(new.media_payload->>'remote_path', '/', 3) <> new.client_id::text then
            raise exception 'Media path does not match message identity';
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists messages_prepare_insert on public.messages;
create trigger messages_prepare_insert
before insert on public.messages
for each row execute procedure public.prepare_message_insert();

create or replace function public.enforce_receipt_progress()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    if new.message_id is distinct from old.message_id
       or new.user_id is distinct from old.user_id then
        raise exception 'Receipt identity cannot be changed';
    end if;

    new.delivered_at := coalesce(old.delivered_at, new.delivered_at, new.read_at);
    new.read_at := coalesce(old.read_at, new.read_at);
    if new.read_at is not null and new.delivered_at is null then
        new.delivered_at := new.read_at;
    end if;
    return new;
end;
$$;

drop trigger if exists receipts_enforce_progress on public.message_receipts;
create trigger receipts_enforce_progress
before update on public.message_receipts
for each row execute procedure public.enforce_receipt_progress();

create or replace function public.enforce_message_update_integrity()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    if new.id is distinct from old.id
       or new.client_id is distinct from old.client_id
       or new.conversation_id is distinct from old.conversation_id
       or new.sender_id is distinct from old.sender_id
       or new.reply_to_id is distinct from old.reply_to_id
       or new.created_at is distinct from old.created_at
       or new.module_type is distinct from old.module_type
       or new.media_type is distinct from old.media_type
       or new.media_payload is distinct from old.media_payload then
        raise exception 'Message identity cannot be changed';
    end if;

    if old.module_type <> 'text'
       and new.deleted_at is null
       and new.body is distinct from old.body then
        raise exception 'Interactive card summary cannot be changed';
    end if;

    if new.module_payload is distinct from old.module_payload then
        if new.module_revision <> old.module_revision + 1 then
            raise exception 'Interactive card revision was not advanced';
        end if;
        if not private.valid_message_module_transition(old.module_type, old.module_payload, new.module_payload, auth.uid()) then
            raise exception 'This card change is not allowed';
        end if;
    elsif new.module_revision <> old.module_revision then
        raise exception 'Interactive card revision cannot change without a payload update';
    end if;
    if not private.valid_message_module(new.module_type, new.module_payload) then
        raise exception 'Interactive card data is invalid';
    end if;
    if not private.valid_message_media(new.media_type, new.media_payload) then
        raise exception 'Media data is invalid';
    end if;

    if old.deleted_at is not null then
        raise exception 'Removed messages cannot be changed';
    end if;

    if new.deleted_at is not null then
        new.body := '';
    elsif btrim(new.body) = '' then
        raise exception 'Message text cannot be empty';
    end if;

    return new;
end;
$$;

drop trigger if exists messages_enforce_update_integrity on public.messages;
create trigger messages_enforce_update_integrity
before update on public.messages
for each row execute procedure public.enforce_message_update_integrity();

create or replace function public.touch_conversation_from_message()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.conversations set updated_at = now() where id = new.conversation_id;
    return new;
end;
$$;

drop trigger if exists messages_touch_conversation on public.messages;
create trigger messages_touch_conversation
after insert or update on public.messages
for each row execute procedure public.touch_conversation_from_message();

alter table public.conversations enable row level security;
alter table public.messages enable row level security;
alter table public.message_receipts enable row level security;
alter table public.message_reactions enable row level security;
alter table public.typing_states enable row level security;
alter table public.presence_states enable row level security;
alter table public.conversation_preferences enable row level security;
alter table public.message_bookmarks enable row level security;

drop policy if exists "Participants can read conversations" on public.conversations;
drop policy if exists "Participants can read messages" on public.messages;
drop policy if exists "Participants can send their messages" on public.messages;
drop policy if exists "Senders can edit their messages" on public.messages;
drop policy if exists "Participants can read receipts" on public.message_receipts;
drop policy if exists "Recipients can create receipts" on public.message_receipts;
drop policy if exists "Recipients can update receipts" on public.message_receipts;
drop policy if exists "Participants can read reactions" on public.message_reactions;
drop policy if exists "Users can add their reaction" on public.message_reactions;
drop policy if exists "Users can remove their reaction" on public.message_reactions;
drop policy if exists "Participants can read typing" on public.typing_states;
drop policy if exists "Users can create their typing state" on public.typing_states;
drop policy if exists "Users can update their typing state" on public.typing_states;
drop policy if exists "Connected profiles can read presence" on public.presence_states;
drop policy if exists "Users can create their presence" on public.presence_states;
drop policy if exists "Users can update their presence" on public.presence_states;
drop policy if exists "Users can read their conversation preferences" on public.conversation_preferences;
drop policy if exists "Users can create their conversation preferences" on public.conversation_preferences;
drop policy if exists "Users can update their conversation preferences" on public.conversation_preferences;
drop policy if exists "Users can delete their conversation preferences" on public.conversation_preferences;
drop policy if exists "Users can read their bookmarks" on public.message_bookmarks;
drop policy if exists "Users can create their bookmarks" on public.message_bookmarks;
drop policy if exists "Users can delete their bookmarks" on public.message_bookmarks;

create policy "Participants can read conversations"
on public.conversations for select to authenticated
using (private.is_conversation_participant(id, auth.uid()));

create policy "Participants can read messages"
on public.messages for select to authenticated
using (private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Participants can send their messages"
on public.messages for insert to authenticated
with check (
    sender_id = auth.uid()
    and edited_at is null
    and deleted_at is null
    and private.is_conversation_participant(conversation_id, auth.uid())
    and (
        reply_to_id is null
        or private.is_reply_in_conversation(reply_to_id, conversation_id)
    )
);

create policy "Senders can edit their messages"
on public.messages for update to authenticated
using (sender_id = auth.uid())
with check (sender_id = auth.uid() and private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Participants can read receipts"
on public.message_receipts for select to authenticated
using (
    exists (
        select 1 from public.messages m
        where m.id = message_id and private.is_conversation_participant(m.conversation_id, auth.uid())
    )
);

create policy "Recipients can create receipts"
on public.message_receipts for insert to authenticated
with check (
    user_id = auth.uid()
    and exists (
        select 1 from public.messages m
        where m.id = message_id
          and m.sender_id <> auth.uid()
          and private.is_conversation_participant(m.conversation_id, auth.uid())
    )
);

create policy "Recipients can update receipts"
on public.message_receipts for update to authenticated
using (
    user_id = auth.uid()
    and exists (
        select 1 from public.messages m
        where m.id = message_id and private.is_conversation_participant(m.conversation_id, auth.uid())
    )
)
with check (
    user_id = auth.uid()
    and exists (
        select 1 from public.messages m
        where m.id = message_id and private.is_conversation_participant(m.conversation_id, auth.uid())
    )
);

create policy "Participants can read reactions"
on public.message_reactions for select to authenticated
using (
    exists (
        select 1 from public.messages m
        where m.id = message_id and private.is_conversation_participant(m.conversation_id, auth.uid())
    )
);

create policy "Users can add their reaction"
on public.message_reactions for insert to authenticated
with check (
    user_id = auth.uid()
    and exists (
        select 1 from public.messages m
        where m.id = message_id and private.is_conversation_participant(m.conversation_id, auth.uid())
    )
);

create policy "Users can remove their reaction"
on public.message_reactions for delete to authenticated
using (
    user_id = auth.uid()
    and exists (
        select 1 from public.messages m
        where m.id = message_id and private.is_conversation_participant(m.conversation_id, auth.uid())
    )
);

create policy "Participants can read typing"
on public.typing_states for select to authenticated
using (private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Users can create their typing state"
on public.typing_states for insert to authenticated
with check (user_id = auth.uid() and private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Users can update their typing state"
on public.typing_states for update to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid() and private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Connected profiles can read presence"
on public.presence_states for select to authenticated
using (user_id = auth.uid() or private.are_connected(auth.uid(), user_id));

create policy "Users can create their presence"
on public.presence_states for insert to authenticated
with check (user_id = auth.uid());

create policy "Users can update their presence"
on public.presence_states for update to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());


create policy "Users can read their conversation preferences"
on public.conversation_preferences for select to authenticated
using (user_id = auth.uid() and private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Users can create their conversation preferences"
on public.conversation_preferences for insert to authenticated
with check (user_id = auth.uid() and private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Users can update their conversation preferences"
on public.conversation_preferences for update to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid() and private.is_conversation_participant(conversation_id, auth.uid()));

create policy "Users can delete their conversation preferences"
on public.conversation_preferences for delete to authenticated
using (user_id = auth.uid());

create policy "Users can read their bookmarks"
on public.message_bookmarks for select to authenticated
using (
    user_id = auth.uid()
    and exists (select 1 from public.messages m where m.id = message_id and private.is_conversation_participant(m.conversation_id, auth.uid()))
);

create policy "Users can create their bookmarks"
on public.message_bookmarks for insert to authenticated
with check (
    user_id = auth.uid()
    and exists (select 1 from public.messages m where m.id = message_id and m.deleted_at is null and private.is_conversation_participant(m.conversation_id, auth.uid()))
);

create policy "Users can delete their bookmarks"
on public.message_bookmarks for delete to authenticated
using (user_id = auth.uid());

revoke all on function public.ensure_direct_conversation(uuid) from public;
revoke all on function public.list_my_conversations() from public;
revoke all on function public.get_conversation_messages(uuid, timestamptz, integer) from public;
revoke all on function public.search_conversation_messages(uuid, text, integer) from public;
revoke all on function public.list_conversation_bookmarks(uuid, integer) from public;
revoke all on function public.list_conversation_links(uuid, integer) from public;
revoke all on function public.list_conversation_media(uuid, integer) from public;
revoke all on function public.mark_conversation_delivered(uuid) from public;
revoke all on function public.mark_conversation_read(uuid) from public;
revoke all on function public.set_conversation_typing(uuid, boolean) from public;
revoke all on function public.set_my_presence(boolean) from public;

grant execute on function public.ensure_direct_conversation(uuid) to authenticated;
grant execute on function public.list_my_conversations() to authenticated;
grant execute on function public.get_conversation_messages(uuid, timestamptz, integer) to authenticated;
grant execute on function public.search_conversation_messages(uuid, text, integer) to authenticated;
grant execute on function public.list_conversation_bookmarks(uuid, integer) to authenticated;
grant execute on function public.list_conversation_links(uuid, integer) to authenticated;
grant execute on function public.list_conversation_media(uuid, integer) to authenticated;
grant execute on function public.mark_conversation_delivered(uuid) to authenticated;
grant execute on function public.mark_conversation_read(uuid) to authenticated;
grant execute on function public.set_conversation_typing(uuid, boolean) to authenticated;
grant execute on function public.set_my_presence(boolean) to authenticated;

alter table public.messages replica identity full;
alter table public.message_receipts replica identity full;
alter table public.message_reactions replica identity full;
alter table public.typing_states replica identity full;
alter table public.presence_states replica identity full;
alter table public.conversations replica identity full;
alter table public.conversation_preferences replica identity full;
alter table public.message_bookmarks replica identity full;

do $$
declare
    table_name text;
begin
    foreach table_name in array array[
        'conversations',
        'messages',
        'message_receipts',
        'message_reactions',
        'message_bookmarks',
        'conversation_preferences',
        'typing_states',
        'presence_states'
    ] loop
        if not exists (
            select 1 from pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = table_name
        ) then
            execute format('alter publication supabase_realtime add table public.%I', table_name);
        end if;
    end loop;
end $$;


-- M6 private Supabase Storage bucket and participant-aware policies.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'chat-media',
    'chat-media',
    false,
    26214400,
    array['image/jpeg','image/png','image/webp','image/gif','audio/mp4','audio/m4a','audio/aac','audio/mpeg','application/pdf','text/plain','text/csv','application/json','application/zip','application/octet-stream','application/vnd.openxmlformats-officedocument.wordprocessingml.document','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet','application/vnd.openxmlformats-officedocument.presentationml.presentation']
)
on conflict (id) do update set
    public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Conversation participants can read chat media" on storage.objects;
drop policy if exists "Senders can upload chat media" on storage.objects;
drop policy if exists "Senders can replace chat media" on storage.objects;
drop policy if exists "Senders can remove chat media" on storage.objects;

create policy "Conversation participants can read chat media"
on storage.objects for select to authenticated
using (
    bucket_id = 'chat-media'
    and exists (
        select 1 from public.conversations conversation
        where conversation.id::text = (storage.foldername(name))[1]
          and private.is_conversation_participant(conversation.id, auth.uid())
    )
);

create policy "Senders can upload chat media"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'chat-media'
    and (storage.foldername(name))[2] = auth.uid()::text
    and exists (
        select 1 from public.conversations conversation
        where conversation.id::text = (storage.foldername(name))[1]
          and private.is_conversation_participant(conversation.id, auth.uid())
    )
);

create policy "Senders can replace chat media"
on storage.objects for update to authenticated
using (bucket_id = 'chat-media' and owner_id = auth.uid()::text)
with check (bucket_id = 'chat-media' and owner_id = auth.uid()::text);

create policy "Senders can remove chat media"
on storage.objects for delete to authenticated
using (bucket_id = 'chat-media' and owner_id = auth.uid()::text);

-- =============================================================
-- M7: shared spaces, channels, membership, reads and reactions
-- =============================================================

create table if not exists public.spaces (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(trim(name)) between 2 and 48),
  description text not null default '' check (char_length(description) <= 240),
  emoji text not null default '✨' check (char_length(emoji) between 1 and 8),
  accent_seed integer not null default 0 check (accent_seed between 0 and 15),
  invite_code text not null unique default ('LN-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8))),
  created_by uuid not null references public.profiles(id) on delete restrict,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.space_members (
  space_id uuid not null references public.spaces(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('owner', 'admin', 'member')),
  joined_at timestamptz not null default now(),
  primary key (space_id, user_id)
);

create unique index if not exists one_owner_per_space
  on public.space_members(space_id) where role = 'owner';

create table if not exists public.space_channels (
  id uuid primary key default gen_random_uuid(),
  space_id uuid not null references public.spaces(id) on delete cascade,
  name text not null check (name ~ '^[a-z0-9][a-z0-9_-]{1,31}$'),
  description text not null default '' check (char_length(description) <= 160),
  kind text not null default 'chat' check (kind in ('chat', 'announcements', 'planning')),
  is_pinned boolean not null default false,
  created_by uuid not null references public.profiles(id) on delete restrict,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (space_id, name)
);

create table if not exists public.space_messages (
  id uuid primary key default gen_random_uuid(),
  channel_id uuid not null references public.space_channels(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete restrict,
  client_id uuid not null,
  body text not null check (char_length(trim(body)) between 1 and 4000),
  reply_to_id uuid references public.space_messages(id) on delete set null,
  created_at timestamptz not null default now(),
  edited_at timestamptz,
  unique (sender_id, client_id)
);

create index if not exists space_messages_channel_created_idx
  on public.space_messages(channel_id, created_at desc);

create table if not exists public.space_message_reactions (
  message_id uuid not null references public.space_messages(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  emoji text not null check (char_length(emoji) between 1 and 16),
  created_at timestamptz not null default now(),
  primary key (message_id, user_id)
);

create table if not exists public.space_channel_reads (
  channel_id uuid not null references public.space_channels(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  read_at timestamptz not null default now(),
  primary key (channel_id, user_id)
);

create table if not exists public.space_preferences (
  space_id uuid not null references public.spaces(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  is_favorite boolean not null default false,
  is_muted boolean not null default false,
  updated_at timestamptz not null default now(),
  primary key (space_id, user_id)
);

create or replace function private.is_space_member(target_space uuid, target_user uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public, private
as $$
  select exists (
    select 1 from public.space_members sm
    where sm.space_id = target_space and sm.user_id = target_user
  );
$$;

create or replace function private.can_manage_space(target_space uuid, target_user uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public, private
as $$
  select exists (
    select 1 from public.space_members sm
    where sm.space_id = target_space
      and sm.user_id = target_user
      and sm.role in ('owner', 'admin')
  );
$$;

create or replace function private.is_space_owner(target_space uuid, target_user uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public, private
as $$
  select exists (
    select 1 from public.space_members sm
    where sm.space_id = target_space
      and sm.user_id = target_user
      and sm.role = 'owner'
  );
$$;

create or replace function private.channel_space(target_channel uuid)
returns uuid
language sql
stable
security definer
set search_path = public, private
as $$
  select sc.space_id from public.space_channels sc where sc.id = target_channel;
$$;

create or replace function private.can_access_space_channel(target_channel uuid, target_user uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public, private
as $$
  select private.is_space_member(private.channel_space(target_channel), target_user);
$$;

create or replace function private.can_post_space_channel(target_channel uuid, target_user uuid default auth.uid())
returns boolean
language sql
stable
security definer
set search_path = public, private
as $$
  select exists (
    select 1
    from public.space_channels sc
    join public.space_members sm on sm.space_id = sc.space_id and sm.user_id = target_user
    where sc.id = target_channel
      and (sc.kind <> 'announcements' or sm.role in ('owner', 'admin'))
  );
$$;

alter table public.spaces enable row level security;
alter table public.space_members enable row level security;
alter table public.space_channels enable row level security;
alter table public.space_messages enable row level security;
alter table public.space_message_reactions enable row level security;
alter table public.space_channel_reads enable row level security;
alter table public.space_preferences enable row level security;

drop policy if exists "space members can read spaces" on public.spaces;
create policy "space members can read spaces" on public.spaces
for select to authenticated using (private.is_space_member(id));

drop policy if exists "space managers can update spaces" on public.spaces;
create policy "space managers can update spaces" on public.spaces
for update to authenticated using (private.can_manage_space(id)) with check (private.can_manage_space(id));

drop policy if exists "members can read memberships" on public.space_members;
create policy "members can read memberships" on public.space_members
for select to authenticated using (private.is_space_member(space_id));

drop policy if exists "managers can add memberships" on public.space_members;
create policy "managers can add memberships" on public.space_members
for insert to authenticated with check (
  private.can_manage_space(space_id)
  and role = 'member'
  and user_id <> auth.uid()
  and exists (
    select 1 from public.connections connection
    where connection.status = 'accepted'
      and (
        (connection.requester_id = auth.uid() and connection.recipient_id = user_id)
        or (connection.recipient_id = auth.uid() and connection.requester_id = user_id)
      )
  )
);

drop policy if exists "managers can update memberships" on public.space_members;
drop policy if exists "owners can update memberships" on public.space_members;
create policy "owners can update memberships" on public.space_members
for update to authenticated
using (private.is_space_owner(space_id))
with check (private.is_space_owner(space_id));

drop policy if exists "managers or self can remove memberships" on public.space_members;
drop policy if exists "owners or self can remove memberships" on public.space_members;
create policy "owners or self can remove memberships" on public.space_members
for delete to authenticated using (
  role <> 'owner'
  and (user_id = auth.uid() or private.is_space_owner(space_id))
);

drop policy if exists "members can read channels" on public.space_channels;
create policy "members can read channels" on public.space_channels
for select to authenticated using (private.is_space_member(space_id));

drop policy if exists "managers can create channels" on public.space_channels;
create policy "managers can create channels" on public.space_channels
for insert to authenticated with check (private.can_manage_space(space_id) and created_by = auth.uid());

drop policy if exists "managers can update channels" on public.space_channels;
create policy "managers can update channels" on public.space_channels
for update to authenticated using (private.can_manage_space(space_id)) with check (private.can_manage_space(space_id));

drop policy if exists "managers can delete channels" on public.space_channels;
create policy "managers can delete channels" on public.space_channels
for delete to authenticated using (private.can_manage_space(space_id));

drop policy if exists "members can read space messages" on public.space_messages;
create policy "members can read space messages" on public.space_messages
for select to authenticated using (private.can_access_space_channel(channel_id));

drop policy if exists "eligible members can send space messages" on public.space_messages;
create policy "eligible members can send space messages" on public.space_messages
for insert to authenticated with check (
  sender_id = auth.uid() and private.can_post_space_channel(channel_id)
);

-- Message editing is intentionally RPC-only. No direct UPDATE policy is exposed,
-- which prevents clients from changing channel, sender or idempotency identity fields.
drop policy if exists "authors can edit space messages" on public.space_messages;

drop policy if exists "members can read space reactions" on public.space_message_reactions;
create policy "members can read space reactions" on public.space_message_reactions
for select to authenticated using (
  exists (
    select 1 from public.space_messages sm
    where sm.id = message_id and private.can_access_space_channel(sm.channel_id)
  )
);

drop policy if exists "members can add space reactions" on public.space_message_reactions;
create policy "members can add space reactions" on public.space_message_reactions
for insert to authenticated with check (
  user_id = auth.uid() and exists (
    select 1 from public.space_messages sm
    where sm.id = message_id and private.can_access_space_channel(sm.channel_id)
  )
);

drop policy if exists "users can remove own space reactions" on public.space_message_reactions;
create policy "users can remove own space reactions" on public.space_message_reactions
for delete to authenticated using (user_id = auth.uid());

drop policy if exists "users can read own channel receipts" on public.space_channel_reads;
create policy "users can read own channel receipts" on public.space_channel_reads
for select to authenticated using (user_id = auth.uid() and private.can_access_space_channel(channel_id));

drop policy if exists "users can write own channel receipts" on public.space_channel_reads;
create policy "users can write own channel receipts" on public.space_channel_reads
for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid() and private.can_access_space_channel(channel_id));

drop policy if exists "users can read own space preferences" on public.space_preferences;
create policy "users can read own space preferences" on public.space_preferences
for select to authenticated using (user_id = auth.uid() and private.is_space_member(space_id));

drop policy if exists "users can write own space preferences" on public.space_preferences;
create policy "users can write own space preferences" on public.space_preferences
for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid() and private.is_space_member(space_id));

create or replace function public.list_my_spaces()
returns table (
  id uuid,
  name text,
  description text,
  emoji text,
  accent_seed integer,
  member_count integer,
  channel_count integer,
  unread_count integer,
  last_activity_at timestamptz,
  my_role text,
  is_favorite boolean,
  is_muted boolean,
  invite_code text
)
language sql
stable
security definer
set search_path = public, private
as $$
  select
    s.id,
    s.name,
    s.description,
    s.emoji,
    s.accent_seed,
    (select count(*)::integer from public.space_members all_members where all_members.space_id = s.id),
    (select count(*)::integer from public.space_channels all_channels where all_channels.space_id = s.id),
    coalesce((
      select count(*)::integer
      from public.space_messages msg
      join public.space_channels unread_channel on unread_channel.id = msg.channel_id
      left join public.space_channel_reads receipt on receipt.channel_id = unread_channel.id and receipt.user_id = auth.uid()
      where unread_channel.space_id = s.id
        and msg.sender_id <> auth.uid()
        and msg.created_at > coalesce(receipt.read_at, '-infinity'::timestamptz)
    ), 0),
    greatest(
      s.updated_at,
      coalesce((
        select max(msg.created_at)
        from public.space_messages msg
        join public.space_channels activity_channel on activity_channel.id = msg.channel_id
        where activity_channel.space_id = s.id
      ), s.created_at)
    ),
    mine.role,
    coalesce(pref.is_favorite, false),
    coalesce(pref.is_muted, false),
    s.invite_code
  from public.spaces s
  join public.space_members mine on mine.space_id = s.id and mine.user_id = auth.uid()
  left join public.space_preferences pref on pref.space_id = s.id and pref.user_id = auth.uid()
  order by coalesce(pref.is_favorite, false) desc, last_activity_at desc;
$$;

create or replace function public.get_space_detail(target_space uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, private
as $$
declare
  space_row jsonb;
  channel_rows jsonb;
  member_rows jsonb;
begin
  if not private.is_space_member(target_space) then
    raise exception 'You no longer have access to this space';
  end if;

  select to_jsonb(item) into space_row
  from (select * from public.list_my_spaces() where id = target_space) item;

  select coalesce(jsonb_agg(to_jsonb(channel_item) order by channel_item.is_pinned desc, channel_item.last_message_at desc nulls last), '[]'::jsonb)
  into channel_rows
  from (
    select
      sc.id,
      sc.space_id,
      sc.name,
      sc.description,
      sc.kind,
      sc.is_pinned,
      coalesce((
        select count(*)::integer from public.space_messages unread_message
        where unread_message.channel_id = sc.id
          and unread_message.sender_id <> auth.uid()
          and unread_message.created_at > coalesce((select read_at from public.space_channel_reads where channel_id = sc.id and user_id = auth.uid()), '-infinity'::timestamptz)
      ), 0) as unread_count,
      coalesce((select body from public.space_messages latest where latest.channel_id = sc.id order by latest.created_at desc limit 1), '') as last_message,
      (select created_at from public.space_messages latest where latest.channel_id = sc.id order by latest.created_at desc limit 1) as last_message_at
    from public.space_channels sc
    where sc.space_id = target_space
  ) channel_item;

  select coalesce(jsonb_agg(to_jsonb(member_item) order by member_item.role_rank, member_item.display_name), '[]'::jsonb)
  into member_rows
  from (
    select
      sm.role,
      sm.joined_at,
      case sm.role when 'owner' then 0 when 'admin' then 1 else 2 end as role_rank,
      p.id,
      p.email,
      p.username,
      p.display_name,
      p.bio,
      p.avatar_seed,
      p.share_code,
      exists(select 1 from public.presence_states ps where ps.user_id = p.id and ps.is_online and ps.last_active_at > now() - interval '2 minutes') as is_online
    from public.space_members sm
    join public.profiles p on p.id = sm.user_id
    where sm.space_id = target_space
  ) member_item;

  return jsonb_build_object('space', space_row, 'channels', channel_rows, 'members', member_rows);
end;
$$;

create or replace function public.create_space(
  space_name text,
  space_description text default '',
  space_emoji text default '✨',
  initial_members uuid[] default '{}'::uuid[]
)
returns uuid
language plpgsql
security definer
set search_path = public, private
as $$
declare
  new_space uuid;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if char_length(trim(space_name)) not between 2 and 48 then raise exception 'Space name must be 2–48 characters'; end if;

  insert into public.spaces(name, description, emoji, accent_seed, created_by)
  values (trim(space_name), left(trim(space_description), 240), coalesce(nullif(trim(space_emoji), ''), '✨'), floor(random() * 8)::integer, auth.uid())
  returning id into new_space;

  insert into public.space_members(space_id, user_id, role) values (new_space, auth.uid(), 'owner');

  insert into public.space_members(space_id, user_id, role)
  select new_space, candidate, 'member'
  from unnest((coalesce(initial_members, '{}'::uuid[]))[1:24]) candidate
  where candidate <> auth.uid()
    and exists (
      select 1 from public.connections c
      where c.status = 'accepted'
        and ((c.requester_id = auth.uid() and c.recipient_id = candidate) or (c.recipient_id = auth.uid() and c.requester_id = candidate))
    )
  on conflict do nothing;

  insert into public.space_channels(space_id, name, description, kind, is_pinned, created_by)
  values (new_space, 'general', 'The main conversation', 'chat', true, auth.uid());

  insert into public.space_preferences(space_id, user_id, is_favorite)
  values (new_space, auth.uid(), true)
  on conflict (space_id, user_id) do update set is_favorite = true, updated_at = now();

  return new_space;
end;
$$;

create or replace function public.create_space_channel(
  target_space uuid,
  channel_name text,
  channel_description text default '',
  channel_kind text default 'chat'
)
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $$
declare
  created public.space_channels;
  normalized text;
begin
  if not private.can_manage_space(target_space) then raise exception 'Only owners and admins can create channels'; end if;
  normalized := trim(both '-' from regexp_replace(lower(trim(channel_name)), '[^a-z0-9_-]+', '-', 'g'));
  if char_length(normalized) not between 2 and 32 then raise exception 'Channel name must be 2–32 characters'; end if;
  if channel_kind not in ('chat', 'announcements', 'planning') then raise exception 'Unsupported channel type'; end if;

  insert into public.space_channels(space_id, name, description, kind, created_by)
  values (target_space, normalized, left(trim(channel_description), 160), channel_kind, auth.uid())
  returning * into created;
  update public.spaces set updated_at = now() where id = target_space;
  return to_jsonb(created);
end;
$$;

create or replace function public.get_space_messages(
  target_channel uuid,
  before_at timestamptz default null,
  page_size integer default 60
)
returns table (
  id uuid,
  client_id uuid,
  channel_id uuid,
  sender_id uuid,
  body text,
  created_at timestamptz,
  edited_at timestamptz,
  reply_to_id uuid,
  reply_preview text,
  sender_username text,
  sender_display_name text,
  sender_avatar_seed integer,
  reactions jsonb,
  is_announcement boolean
)
language sql
stable
security definer
set search_path = public, private
as $$
  select
    msg.id,
    msg.client_id,
    msg.channel_id,
    msg.sender_id,
    msg.body,
    msg.created_at,
    msg.edited_at,
    msg.reply_to_id,
    coalesce(reply.body, ''),
    sender.username,
    sender.display_name,
    sender.avatar_seed,
    coalesce((
      select jsonb_agg(jsonb_build_object(
        'emoji', grouped.emoji,
        'count', grouped.total,
        'reacted_by_me', grouped.reacted_by_me
      ) order by grouped.total desc, grouped.emoji)
      from (
        select reaction.emoji, count(*)::integer as total, bool_or(reaction.user_id = auth.uid()) as reacted_by_me
        from public.space_message_reactions reaction
        where reaction.message_id = msg.id
        group by reaction.emoji
      ) grouped
    ), '[]'::jsonb),
    channel.kind = 'announcements'
  from public.space_messages msg
  join public.space_channels channel on channel.id = msg.channel_id
  join public.profiles sender on sender.id = msg.sender_id
  left join public.space_messages reply on reply.id = msg.reply_to_id
  where msg.channel_id = target_channel
    and private.can_access_space_channel(target_channel)
    and (before_at is null or msg.created_at < before_at)
  order by msg.created_at desc
  limit greatest(1, least(page_size, 100));
$$;

create or replace function public.send_space_message(
  target_channel uuid,
  client_message_id uuid,
  message_body text,
  reply_to_message uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $$
declare
  sent_id uuid;
  result jsonb;
begin
  if not private.can_post_space_channel(target_channel) then raise exception 'You cannot post in this channel'; end if;
  if char_length(trim(message_body)) not between 1 and 4000 then raise exception 'Message cannot be empty and must stay under 4000 characters'; end if;
  if reply_to_message is not null and not exists (
    select 1 from public.space_messages reply where reply.id = reply_to_message and reply.channel_id = target_channel
  ) then raise exception 'The replied message is no longer available'; end if;

  insert into public.space_messages(channel_id, sender_id, client_id, body, reply_to_id)
  values (target_channel, auth.uid(), client_message_id, trim(message_body), reply_to_message)
  on conflict (sender_id, client_id) do update set client_id = excluded.client_id
  returning id into sent_id;

  update public.space_channels set updated_at = now() where id = target_channel;
  update public.spaces set updated_at = now() where id = private.channel_space(target_channel);

  select to_jsonb(message_row) into result
  from public.get_space_messages(target_channel, null, 100) message_row
  where message_row.id = sent_id;
  return result;
end;
$$;

create or replace function public.react_space_message(target_message uuid, reaction_emoji text default null)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare
  target_channel uuid;
begin
  select channel_id into target_channel from public.space_messages where id = target_message;
  if target_channel is null or not private.can_access_space_channel(target_channel) then raise exception 'Message not found'; end if;
  delete from public.space_message_reactions where message_id = target_message and user_id = auth.uid();
  if reaction_emoji is not null and char_length(trim(reaction_emoji)) between 1 and 16 then
    insert into public.space_message_reactions(message_id, user_id, emoji)
    values (target_message, auth.uid(), trim(reaction_emoji));
  end if;
end;
$$;

create or replace function public.set_space_preferences(target_space uuid, favorite boolean, muted boolean)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
begin
  if not private.is_space_member(target_space) then raise exception 'Space not found'; end if;
  insert into public.space_preferences(space_id, user_id, is_favorite, is_muted, updated_at)
  values (target_space, auth.uid(), favorite, muted, now())
  on conflict (space_id, user_id) do update
  set is_favorite = excluded.is_favorite, is_muted = excluded.is_muted, updated_at = now();
end;
$$;

create or replace function public.mark_space_channel_read(target_channel uuid)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
begin
  if not private.can_access_space_channel(target_channel) then raise exception 'Channel not found'; end if;
  insert into public.space_channel_reads(channel_id, user_id, read_at)
  values (target_channel, auth.uid(), now())
  on conflict (channel_id, user_id) do update set read_at = now();
end;
$$;

create or replace function public.leave_space(target_space uuid)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare
  member_role text;
begin
  select role into member_role from public.space_members where space_id = target_space and user_id = auth.uid();
  if member_role is null then return; end if;
  if member_role = 'owner' then raise exception 'Transfer ownership before leaving this space'; end if;
  delete from public.space_members where space_id = target_space and user_id = auth.uid();
end;
$$;

revoke all on function private.is_space_member(uuid, uuid) from public;
revoke all on function private.can_manage_space(uuid, uuid) from public;
revoke all on function private.is_space_owner(uuid, uuid) from public;
revoke all on function private.channel_space(uuid) from public;
revoke all on function private.can_access_space_channel(uuid, uuid) from public;
revoke all on function private.can_post_space_channel(uuid, uuid) from public;

revoke all on function public.list_my_spaces() from public;
revoke all on function public.get_space_detail(uuid) from public;
revoke all on function public.create_space(text, text, text, uuid[]) from public;
revoke all on function public.create_space_channel(uuid, text, text, text) from public;
revoke all on function public.get_space_messages(uuid, timestamptz, integer) from public;
revoke all on function public.send_space_message(uuid, uuid, text, uuid) from public;
revoke all on function public.react_space_message(uuid, text) from public;
revoke all on function public.set_space_preferences(uuid, boolean, boolean) from public;
revoke all on function public.mark_space_channel_read(uuid) from public;
revoke all on function public.leave_space(uuid) from public;

grant execute on function public.list_my_spaces() to authenticated;
grant execute on function public.get_space_detail(uuid) to authenticated;
grant execute on function public.create_space(text, text, text, uuid[]) to authenticated;
grant execute on function public.create_space_channel(uuid, text, text, text) to authenticated;
grant execute on function public.get_space_messages(uuid, timestamptz, integer) to authenticated;
grant execute on function public.send_space_message(uuid, uuid, text, uuid) to authenticated;
grant execute on function public.react_space_message(uuid, text) to authenticated;
grant execute on function public.set_space_preferences(uuid, boolean, boolean) to authenticated;
grant execute on function public.mark_space_channel_read(uuid) to authenticated;
grant execute on function public.leave_space(uuid) to authenticated;

do $$
begin
  alter publication supabase_realtime add table public.spaces;
exception when duplicate_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.space_members;
exception when duplicate_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.space_channels;
exception when duplicate_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.space_messages;
exception when duplicate_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.space_message_reactions;
exception when duplicate_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.space_channel_reads;
exception when duplicate_object then null;
end $$;
do $$
begin
  alter publication supabase_realtime add table public.space_preferences;
exception when duplicate_object then null;
end $$;
