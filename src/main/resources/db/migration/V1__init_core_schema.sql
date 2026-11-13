CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    version_name VARCHAR(150) NOT NULL,
    raw_content TEXT NOT NULL,
    embedding_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE job_applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    resume_id UUID REFERENCES resumes(id),
    company_name VARCHAR(200) NOT NULL,
    role_title VARCHAR(200) NOT NULL,
    work_mode VARCHAR(50) NOT NULL,
    salary_min INTEGER,
    salary_max INTEGER,
    currency VARCHAR(3),
    status VARCHAR(50) NOT NULL,
    job_description TEXT,
    applied_at TIMESTAMP WITH TIME ZONE,
    last_status_change_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE interview_rounds (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    round_type VARCHAR(50) NOT NULL,
    outcome VARCHAR(50) NOT NULL,
    preparation_notes TEXT,
    questions_asked TEXT,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_job_applications_user_id ON job_applications(user_id);
CREATE INDEX idx_interview_rounds_app_id ON interview_rounds(application_id);
