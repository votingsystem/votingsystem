CREATE USER "user" PASSWORD 'user';

drop database IF EXISTS "AccessControl";
drop database IF EXISTS "ControlCenter";
drop database IF EXISTS "TimeStampServer";
drop database IF EXISTS "CurrencyServer";

CREATE DATABASE "AccessControl";
GRANT ALL PRIVILEGES ON DATABASE "AccessControl" TO "user";

CREATE DATABASE "ControlCenter";
GRANT ALL PRIVILEGES ON DATABASE "ControlCenter" TO "user";

CREATE DATABASE "TimeStampServer";
GRANT ALL PRIVILEGES ON DATABASE "TimeStampServer" TO "user";

CREATE DATABASE "CurrencyServer";
GRANT ALL PRIVILEGES ON DATABASE "CurrencyServer" TO "user";
