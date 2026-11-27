package com.example.data.remote

object SupabaseSchemaSql {
    val SCHEMA_SQL: String = """
        -- StudyHub Supabase PostgreSQL Schema
        -- Firebase is used strictly for Authentication; all application data is stored in Supabase.
        -- Paste & run this script in Supabase Dashboard -> SQL Editor.

        create table if not exists public.user_profiles (
            id text primary key,
            user_id text not null,
            display_name text not null default 'Student',
            email text not null default '',
            selected_class bigint not null default 7,
            role text not null default 'student',
            subscription_tier text not null default 'free',
            avg_score bigint not null default 87,
            day_streak bigint not null default 15,
            points bigint not null default 120,
            daily_goal_count bigint not null default 5,
            daily_completed_count bigint not null default 3,
            dark_theme_enabled boolean not null default false,
            notifications_enabled boolean not null default true,
            exam_reminders_enabled boolean not null default true,
            new_materials_enabled boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.classes (
            id text primary key,
            grade bigint not null,
            name text not null,
            description text not null default '',
            student_count bigint not null default 0,
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.subjects (
            id text primary key,
            class_grade bigint not null,
            name text not null,
            subtitle text not null default '',
            icon_key text not null default 'book',
            color_key text not null default 'blue',
            chapter_count bigint not null default 0,
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.chapters (
            id text primary key,
            class_grade bigint not null,
            subject_id text not null default '',
            subject_name text not null default '',
            chapter_number bigint not null default 1,
            title text not null,
            summary text not null default '',
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.study_materials (
            id text primary key,
            class_grade bigint not null,
            subject_id text not null default '',
            subject_name text not null default '',
            chapter_id text not null default '',
            chapter_title text not null default '',
            title text not null,
            description text not null default '',
            material_type text not null default 'short_notes',
            content_markdown text not null default '',
            file_size_label text not null default '1.2 MB',
            tags jsonb not null default '[]'::jsonb,
            important boolean not null default false,
            premium_only boolean not null default false,
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.quizzes (
            id text primary key,
            class_grade bigint not null,
            subject_id text not null default '',
            subject_name text not null default '',
            chapter_id text not null default '',
            chapter_title text not null default '',
            title text not null,
            description text not null default '',
            duration_minutes bigint not null default 10,
            question_count bigint not null default 5,
            premium_only boolean not null default false,
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.quiz_questions (
            id text primary key,
            quiz_id text not null references public.quizzes(id) on delete cascade,
            question_type text not null default 'mcq',
            prompt text not null,
            options jsonb not null default '[]'::jsonb,
            correct_indices jsonb not null default '[]'::jsonb,
            correct_text text not null default '',
            explanation text not null default '',
            points bigint not null default 10,
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.exam_papers (
            id text primary key,
            class_grade bigint not null,
            subject_id text not null default '',
            subject_name text not null default '',
            title text not null,
            paper_category text not null default 'previous_year',
            year bigint not null default 2025,
            file_size_label text not null default '2.4 MB',
            content_text text not null default '',
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.paper_attachments (
            id text primary key,
            parent_id text not null,
            group_id text not null,
            kind text not null default 'image',
            file_name text not null,
            mime_type text not null default 'image/jpeg',
            size_bytes bigint not null default 0,
            chunk_index bigint not null default 0,
            total_chunks bigint not null default 1,
            sort_order bigint not null default 0,
            published boolean not null default true,
            data_base64 text not null default '',
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.announcements (
            id text primary key,
            title text not null,
            message text not null,
            target_class bigint not null default 0,
            priority text not null default 'normal',
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.app_settings (
            id text primary key,
            premium_monthly_price text not null default '${'$'}4.99/mo',
            premium_yearly_price text not null default '${'$'}39.99/yr',
            ads_enabled boolean not null default true,
            ad_family_safe_mode boolean not null default true,
            ad_placement_home boolean not null default true,
            ad_placement_materials boolean not null default true,
            ad_placement_quizzes boolean not null default false,
            ad_placement_papers boolean not null default true,
            ad_banner_title text not null default 'StudyHub Plus • Family-Safe Learning',
            ad_banner_subtitle text not null default 'Unlock unlimited offline packs, advanced analytics & ad-free mode.',
            ad_cta_label text not null default 'Upgrade',
            ad_network_provider text not null default 'StudyHub Direct (Family-Safe)',
            maintenance_mode boolean not null default false,
            support_email text not null default 'support@studyhub.edu',
            published boolean not null default true,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.bookmarks (
            id text primary key,
            user_id text not null,
            material_id text not null,
            title text not null,
            subject_name text not null default '',
            class_grade bigint not null default 7,
            material_type text not null default 'short_notes',
            subtitle text not null default '',
            accent_key text not null default 'blue',
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.quiz_attempts (
            id text primary key,
            user_id text not null,
            quiz_id text not null,
            quiz_title text not null default '',
            subject_name text not null default '',
            class_grade bigint not null default 7,
            mode text not null default 'practice',
            total_questions bigint not null default 0,
            correct_answers bigint not null default 0,
            score_percent bigint not null default 0,
            duration_seconds bigint not null default 0,
            points_earned bigint not null default 0,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.reading_progress (
            id text primary key,
            user_id text not null,
            material_id text not null,
            title text not null default '',
            subject_name text not null default '',
            chapter_title text not null default '',
            class_grade bigint not null default 7,
            progress_percent bigint not null default 0,
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create table if not exists public.downloads (
            id text primary key,
            user_id text not null,
            material_id text not null,
            title text not null default '',
            subject_name text not null default '',
            class_grade bigint not null default 7,
            file_size_label text not null default '1.5 MB',
            created_at timestamptz not null default now(),
            updated_at timestamptz not null default now()
        );

        create index if not exists idx_subjects_grade on public.subjects (class_grade);
        create index if not exists idx_chapters_subject on public.chapters (subject_id);
        create index if not exists idx_materials_grade on public.study_materials (class_grade);
        create index if not exists idx_questions_quiz on public.quiz_questions (quiz_id);
        create index if not exists idx_papers_grade on public.exam_papers (class_grade);
        create index if not exists idx_attachments_parent on public.paper_attachments (parent_id);
        create index if not exists idx_bookmarks_user on public.bookmarks (user_id);
        create index if not exists idx_attempts_user on public.quiz_attempts (user_id);
        create index if not exists idx_progress_user on public.reading_progress (user_id);
        create index if not exists idx_downloads_user on public.downloads (user_id);

        grant usage on schema public to anon, authenticated;
        grant select, insert, update, delete on all tables in schema public to anon, authenticated;

        alter table public.user_profiles disable row level security;
        alter table public.classes disable row level security;
        alter table public.subjects disable row level security;
        alter table public.chapters disable row level security;
        alter table public.study_materials disable row level security;
        alter table public.quizzes disable row level security;
        alter table public.quiz_questions disable row level security;
        alter table public.exam_papers disable row level security;
        alter table public.paper_attachments disable row level security;
        alter table public.announcements disable row level security;
        alter table public.app_settings disable row level security;
        alter table public.bookmarks disable row level security;
        alter table public.quiz_attempts disable row level security;
        alter table public.reading_progress disable row level security;
        alter table public.downloads disable row level security;
    """.trimIndent()
}
