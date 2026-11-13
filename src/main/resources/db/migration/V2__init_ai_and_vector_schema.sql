CREATE TABLE resume_chunks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding_data TEXT NOT NULL
);

CREATE INDEX idx_resume_chunks_user_resume ON resume_chunks(user_id, resume_id);

CREATE TABLE ai_assessments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES job_applications(id) ON DELETE CASCADE,
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    match_score INTEGER NOT NULL,
    missing_skills TEXT,
    matching_skills TEXT,
    tailoring_suggestions TEXT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_assessments_app_id ON ai_assessments(application_id);
CREATE INDEX idx_ai_assessments_user_id ON ai_assessments(user_id);
