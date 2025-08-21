-- Migration: Create users table with PostgreSQL Row-Level Security (RLS)
-- This implements the Pool Model for multi-tenancy where all tenants share the same schema
-- but data is isolated using PostgreSQL's Row-Level Security feature.

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    age INTEGER,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure email uniqueness within tenant
    CONSTRAINT uk_users_email_tenant UNIQUE (email, tenant_id)
);

-- Create user_roles table for role management
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    
    -- Foreign key to users table
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Ensure role uniqueness per user
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role)
);

-- Create indexes for performance optimization
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Enable Row-Level Security on users table (forced for superusers)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- Create RLS policy for users table
-- This policy ensures that users can only access data for their current tenant
CREATE POLICY tenant_isolation_policy ON users
    FOR ALL
    TO PUBLIC
    USING (tenant_id = current_setting('app.current_tenant', true));

-- Enable Row-Level Security on user_roles table (forced for superusers)
ALTER TABLE user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles FORCE ROW LEVEL SECURITY;

-- Create RLS policy for user_roles table
-- This policy ensures that user roles are also tenant-isolated
-- It joins with the users table to inherit the tenant context
CREATE POLICY tenant_isolation_policy ON user_roles
    FOR ALL
    TO PUBLIC
    USING (
        EXISTS (
            SELECT 1 FROM users 
            WHERE users.id = user_roles.user_id 
            AND users.tenant_id = current_setting('app.current_tenant', true)
        )
    );

-- Create function to set current tenant
-- This function will be called by our application to set the tenant context
CREATE OR REPLACE FUNCTION set_current_tenant(tenant_id TEXT)
RETURNS void AS $$
BEGIN
    PERFORM set_config('app.current_tenant', tenant_id, true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create function to get current tenant
-- This function can be used in queries to get the current tenant
CREATE OR REPLACE FUNCTION get_current_tenant()
RETURNS TEXT AS $$
BEGIN
    RETURN current_setting('app.current_tenant', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create updated_at trigger function
-- This function automatically updates the updated_at column on row updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for users table
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Grant necessary permissions
-- These grants ensure that the application can perform all necessary operations
GRANT USAGE ON SCHEMA public TO PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON users TO PUBLIC;
GRANT SELECT, INSERT, UPDATE, DELETE ON user_roles TO PUBLIC;
GRANT USAGE, SELECT ON SEQUENCE users_id_seq TO PUBLIC;
GRANT EXECUTE ON FUNCTION set_current_tenant(TEXT) TO PUBLIC;
GRANT EXECUTE ON FUNCTION get_current_tenant() TO PUBLIC;

-- Insert sample data for testing (will be removed in production)
-- This data helps verify that RLS is working correctly
INSERT INTO users (tenant_id, name, email, age, password, enabled) VALUES
('tenant1', 'John Doe', 'john@tenant1.com', 25, '$2a$12$dummy.hash.for.testing', true),
('tenant2', 'Jane Smith', 'jane@tenant2.com', 30, '$2a$12$dummy.hash.for.testing', true);

INSERT INTO user_roles (user_id, role) VALUES
(1, 'USER'),
(1, 'ADMIN'),
(2, 'USER');

-- Add comments for documentation
COMMENT ON TABLE users IS 'Users table with multi-tenant support using PostgreSQL RLS';
COMMENT ON TABLE user_roles IS 'User roles table with tenant isolation via RLS';
COMMENT ON FUNCTION set_current_tenant(TEXT) IS 'Sets the current tenant context for RLS policies';
COMMENT ON FUNCTION get_current_tenant() IS 'Gets the current tenant context';
COMMENT ON POLICY tenant_isolation_policy ON users IS 'RLS policy ensuring tenant data isolation';
COMMENT ON POLICY tenant_isolation_policy ON user_roles IS 'RLS policy ensuring user roles are tenant-isolated';
