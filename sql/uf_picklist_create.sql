-- ---------------------------------------------------------------------
-- config.uf_picklist_create
--
-- This function creates picklist records for an array of items
--
-- author: MJBoldin <raidersmj@yahoo.com>
-- ---------------------------------------------------------------------

DROP FUNCTION IF EXISTS config.uf_picklist_create(jsonb[], text, bigint, OUT jsonb[]);

CREATE FUNCTION config.uf_picklist_create(picklistObj jsonb[], tableName text, 
                        userid bigint, OUT result jsonb[]) returns jsonb[]
    SECURITY definer
    LANGUAGE plpgsql
AS
$$
DECLARE
    jData      JSONB;
    picklistId BIGINT;
BEGIN
    RAISE NOTICE 'Table name in %, JSON In is picklistObj %', tableName, picklistObj;

    -- check to see if table exists already, if it does throw an error
    IF EXISTS (SELECT 1 FROM config.picklist WHERE lookup_table = UPPER(tableName)) THEN
        RAISE EXCEPTION 'Lookup already exists for table name %',UPPER(tableName);
    END IF;

    -- parse json data and insert each item into the picklist table
    RAISE NOTICE '  Parsing the json';
    FOREACH jData IN ARRAY (picklistObj)
    LOOP
        RAISE NOTICE '  &&&&&&&& got data row of %', jData;
        INSERT INTO config.picklist (
              lookup_cd
            , lookup_short_desc
            , lookup_long_desc
            , lookup_table
            , lookup_order
            , capco_marking
            , augment
            , metadata
        ) VALUES (
            (jData::json->>'lookupCd')
            , (jData::json->>'lookupShortDesc')
            , (jData::json->>'lookupLongDesc')
            , UPPER(tableName)
            , (jData::json->>'lookupOrder')::BIGINT
            , (jData::json->>'capcoMarking')
            , (jData::json->>'augment')
            , (jData::json->>'metadata')::JSONB
            ) RETURNING lookup_id INTO picklistId;

            RAISE NOTICE 'Got new id [%] for picklist item', picklistId;

            -- update the lookup table with the user and date by id and deleted
            UPDATE helios.lookup
            SET
                deleteflag = 'Y'
                , data_creator_id = userId
                , data_created_dt = now()
                , data_modified_id= userId
                , data_modified_dt= now()
            WHERE
                lookup_id = picklistId;

        -- select row and add it to the return array
        result := array_append( result, (SELECT row_to_json(picklist) 
                                            FROM config.picklist 
                                            WHERE lookup_id=picklistId 
                                            AND lookup_table=UPPER(tableName))::jsonb);
    END LOOP;
    RAISE NOTICE 'CREATED PICKLISTS of %',result;
END
$$;

COMMENT ON FUNCTION config.uf_picklist_create(jsonb[], text, bigint, OUT jsonb[]) IS 
    'This function is used to insert a new picklist value';

ALTER FUNCTION config.uf_picklist_create(jsonb[], text, bigint, OUT jsonb[]) OWNER TO pegasus;