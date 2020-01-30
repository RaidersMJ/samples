-- ---------------------------------------------------------------------
-- operations.trigger_fct_le_operation_stbl
--
-- This trigger function inserts ops data into the corresponding shadow
-- table.
--
-- author: MJBoldin <raidersmj@yahoo.com>
-- ---------------------------------------------------------------------

DROP FUNCTION IF EXISTS operations.trigger_fct_le_operation_stbl();

CREATE OR REPLACE FUNCTION operations.trigger_fct_le_operation_stbl()
    RETURNS TRIGGER AS
$BODY$
BEGIN
    DECLARE
    BEGIN
        -- on an insert, update the modify date to the create date
        IF TG_OP = 'INSERT'
        THEN
            NEW.modify_dtg := NEW.create_dtg;
        END IF;

        -- on an update, update the modify date
        IF TG_OP = 'UPDATE'
        THEN
            SELECT LOCALTIMESTAMP INTO NEW.modify_dtg;
        END IF;

        -- insert new record into shadow table
        INSERT INTO operations.le_operation_stbl (
            operation_name,
            agency_id,
            create_dtg,
            create_user,
            deleted,
            modify_dtg,
            modify_user
        ) VALUES (
            NEW.operation_name,
            NEW.agency_id,
            NEW.create_dtg,
            NEW.create_user,
            NEW.deleted,
            NEW.modify_dtg,
            NEW.modify_user
        );
    END;
    RETURN NEW;
END;
$BODY$
LANGUAGE plpgsql VOLATILE SECURITY DEFINER
COST 100;

ALTER FUNCTION operations.trigger_fct_le_operation_stbl()
    OWNER TO pegasus;
GRANT EXECUTE ON FUNCTION operations.trigger_fct_le_operation_stbl() TO pegasus;
GRANT EXECUTE ON FUNCTION operations.trigger_fct_le_operation_stbl() TO 
    read_write_operations;