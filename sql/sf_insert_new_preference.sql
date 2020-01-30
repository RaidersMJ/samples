-- ---------------------------------------------------------------------
-- helios.sf_insert_new_preference
--
-- This function inserts a new preference either at the global or page
-- level in the account table.
--
-- author: MJBoldin <raidersmj@yahoo.com>
-- ---------------------------------------------------------------------

DROP FUNCTION IF EXISTS helios.sf_insert_new_preference(text, text, text, OUT text);

CREATE OR REPLACE FUNCTION helios.sf_insert_new_preference(
  IN key text,
  IN value text,
  IN pageName text default null,
  OUT message text)
    RETURNS text
    LANGUAGE 'plpgsql'

    COST 100
    VOLATILE SECURITY DEFINER
AS $BODY$

  DECLARE
    path	        text[];
    totalRowCount	numeric;
    jsonString    text;
    results       text;
  BEGIN
    RAISE INFO '=============    Insert new user preference    =============';

    key := lower(key);

    -- if pageName is not null
    IF (pageName IS NOT NULL) THEN
      pageName := lower(pageName);

      -- create array of json insert structure      
      path := array_append(path, 'pages');
      path := array_append(path, pageName);

      -- check to see if the json pages element has been previously inserted
      jsonString := '''pages''->''' || pageName || '''';
      EXECUTE format('SELECT user_preferences::jsonb->%s AS results FROM helios.account LIMIT 1;', 
          jsonString) INTO results;

      -- if the json pages element doesn't already exist insert a blank object
      IF (results IS NULL) THEN
          UPDATE helios.account
            SET user_preferences = jsonb_insert(user_preferences, path, '{}'::jsonb);
      END IF;

      -- add key to pages element
      path := array_append(path, key);
      jsonString := '''pages''->''' || pageName || '''->''' || key || '''';

      -- build return message
      message = key || ' inserted into pages at ' || pageName || ' and set to ' || value || '.';

    ELSE
      -- otherwise insert a global preference
      path := array_append(path, 'globals');

      -- add key to global element
      path := array_append(path, key);

      jsonString := '''globals''->''' || key || '''';

      -- build return message
      message = key || ' inserted into globals and set to ' || value || '.';
    END IF;

    -- check to see if new preferences had been already inserted at appropriate level
    EXECUTE format('SELECT user_preferences::jsonb->%s AS results FROM helios.account LIMIT 1;', 
      jsonString) INTO results;

    -- if it doesn't already exist
    IF (results IS NULL) THEN
        -- update the account table with the new preference and the modified date
        UPDATE helios.account
            SET user_preferences = jsonb_insert(user_preferences, path, value::jsonb),
                data_modified_dt = now();

        -- retrieve the number of updated account rows and return message with that number
        GET DIAGNOSTICS totalRowCount = ROW_COUNT;
        message = message || ' (' || totalRowCount || ')';

    ELSE
      -- return message saying preference was already added
      message = jsonString || ' was previously inserted.';
    END IF;
  END;
$BODY$;

COMMENT ON FUNCTION helios.sf_insert_new_preference(text, text, text, OUT text) IS 
    'This function is used to insert a new preference';

ALTER FUNCTION helios.sf_insert_new_preference(text, text, text, OUT text) 
    OWNER TO pegasus;