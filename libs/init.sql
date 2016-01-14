CREATE USER "userVS" PASSWORD 'userVS';

drop database IF EXISTS "AccessControl";
drop database IF EXISTS "ControlCenter";
drop database IF EXISTS "TimeStampServer";
drop database IF EXISTS "CurrencyServer";

CREATE DATABASE "AccessControl";
GRANT ALL PRIVILEGES ON DATABASE "AccessControl" TO "userVS";

CREATE DATABASE "ControlCenter";
GRANT ALL PRIVILEGES ON DATABASE "ControlCenter" TO "userVS";

CREATE DATABASE "TimeStampServer";
GRANT ALL PRIVILEGES ON DATABASE "TimeStampServer" TO "userVS";

CREATE DATABASE "CurrencyServer";
GRANT ALL PRIVILEGES ON DATABASE "CurrencyServer" TO "userVS";
