-- ---------------------------------------------------------------------
-- helios.sf_update_jsonb
--
-- This function merges 2 jsonb objects.
--
-- author: MJBoldin <raidersmj@yahoo.com>
-- ---------------------------------------------------------------------

DROP FUNCTION IF EXISTS helios.sf_update_jsonb(jsonb, jsonb);

CREATE OR REPLACE FUNCTION helios.sf_update_jsonb(
    val1 jsonb,
    val2 jsonb)
RETURNS jsonb
AS $BODY$
DECLARE
    result jsonb;
    rec    RECORD;
BEGIN
    -- return first jsonb object if second is null
    IF jsonb_typeof(val2) = 'null' THEN
        RETURN val1;
    END IF;

    -- initialize result to first jsonb object
    result = val1;

    -- loop through each object in second jsonb object
    FOR rec IN SELECT key, value FROM jsonb_each(val2) LOOP
        -- the key is an object so add it recursively to the results
        IF jsonb_typeof(val2->rec.key) = 'object' THEN
            result = result || jsonb_build_object(rec.key, 
                helios.sf_update_jsonb(val1->rec.key, val2->rec.key));
        ELSE
            -- the key is not an object so add it to the result
            result = result || jsonb_build_object(rec.key, rec.value);
        END IF;
    END LOOP;

    -- return merged objects
    RETURN result;
END;
$BODY$
LANGUAGE plpgsql;

COMMENT ON FUNCTION helios.sf_update_jsonb(jsonb, jsonb) IS 
    'This function is used to merge two json objects';

ALTER FUNCTION helios.sf_update_jsonb(jsonb, jsonb) OWNER TO pegasus;