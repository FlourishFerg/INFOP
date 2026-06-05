-- =========================================================
-- 1. USERS
-- =========================================================
CREATE TABLE users (
    id              VARCHAR(50)  PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'STUDENT',
    is_enabled      BOOLEAN      DEFAULT TRUE,
    is_verified     BOOLEAN      DEFAULT FALSE,
    last_login_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 2. PROFILES  (one-to-one with users)
-- =========================================================
CREATE TABLE profiles (
    id                      VARCHAR(50)  PRIMARY KEY,
    user_id                 VARCHAR(50)  NOT NULL UNIQUE,
    full_name               VARCHAR(255) NOT NULL,
    phone_number            VARCHAR(50),
    country                 VARCHAR(100),
    geopolitical_zone       VARCHAR(100),
    state                   VARCHAR(100),
    city                    VARCHAR(100),
    profession              VARCHAR(255),
    profile_type            VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
    academic_qualification  VARCHAR(255),
    gender                  VARCHAR(20),
    date_of_birth           DATE,
    membership_type         VARCHAR(50)  DEFAULT 'FREE',
    academic_institution    VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================================================
-- 3. AUTH TOKENS
-- =========================================================
CREATE TABLE auth_tokens (
    id           VARCHAR(50)  PRIMARY KEY,
    token_value  VARCHAR(500) NOT NULL UNIQUE,
    token_type   VARCHAR(50)  NOT NULL,
    user_id      VARCHAR(50)  NOT NULL,
    expiry_date  TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked   BOOLEAN      DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auth_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =========================================================
-- 4. INDEXES
-- =========================================================
CREATE INDEX idx_users_email              ON users(email);
CREATE INDEX idx_profiles_user_id         ON profiles(user_id);
CREATE INDEX idx_auth_tokens_user_id      ON auth_tokens(user_id);
CREATE INDEX idx_auth_tokens_token_value  ON auth_tokens(token_value);
CREATE INDEX idx_auth_tokens_token_type   ON auth_tokens(token_type);
CREATE INDEX idx_auth_tokens_expiry_date  ON auth_tokens(expiry_date);
