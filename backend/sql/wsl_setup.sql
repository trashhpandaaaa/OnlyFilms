-- WSL MySQL Setup: configure root for password-less access
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
FLUSH PRIVILEGES;
SELECT 'Root user configured for password-less access' AS status;
