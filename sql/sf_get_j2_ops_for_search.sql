-- ---------------------------------------------------------------------
-- operations.sf_get_j2_ops_for_search
--
-- This function returns j2 ops based upon search criteria.
--
-- author: MJBoldin <raidersmj@yahoo.com>
-- ---------------------------------------------------------------------

-- Function: operations.sf_get_j2_ops_for_search
-- DROP VIEW operations.sf_get_j2_ops_for_search;

DROP FUNCTION IF EXISTS operations.sf_get_j2_ops_for_search(text, bigint, bigint, 
                            bigint, date, date);

CREATE OR REPLACE FUNCTION operations.sf_getj2opsforsearch(
    opname text,
    opstatus bigint,
    optype bigint,
    agency bigint,
    startdt date,
    enddt date)
  RETURNS SETOF operations.le_operations_view AS
$BODY$
    DECLARE
      criteriaCount numeric;
      queryString   text;
    BEGIN
      RAISE INFO '=============    Search J2 Operations    =============';

      criteriaCount := 0;

      -- build query
      queryString = 'SELECT * FROM operations.le_operations_view';

      IF (opName IS NOT NULL) OR (opStatus IS NOT NULL) OR (opType IS NOT NULL) OR (agency IS NOT NULL) OR
          (startDt IS NOT NULL) OR (endDt IS NOT NULL) THEN

          RAISE INFO 'Criteria Included';
          queryString = queryString || ' WHERE ';

          -- name
          IF (opName IS NOT NULL) THEN
             queryString = queryString || ' operation_name ilike ''%' || opName || '%'' ';
             criteriaCount := criteriaCount+1;
          END IF;

          -- status
          IF (opStatus IS NOT NULL) THEN
              IF (criteriaCount > 0) THEN
                  queryString = queryString || ' AND ';
              END IF;
             queryString = queryString || ' operation_status = ' || opStatus ;
             criteriaCount := criteriaCount+1;
          END IF;

          -- type
          IF (opType IS NOT NULL) THEN
              IF (criteriaCount > 0) THEN
                  queryString = queryString || ' AND ';
              END IF;
             queryString = queryString || ' operation_type_id = ' || opType ;
             criteriaCount := criteriaCount+1;
           END IF;

          -- agency
          IF (agency IS NOT NULL) THEN
              IF (criteriaCount > 0) THEN
                  queryString = queryString || ' AND ';
              END IF;
             queryString = queryString || ' agency_id = ' || agency ;
             criteriaCount := criteriaCount+1;
          END IF;

          -- date range
          IF (startDt IS NOT NULL OR endDt IS NOT NULL) THEN
              IF (criteriaCount > 0) THEN
                  queryString = queryString || ' AND ';
              END IF;

              IF (startDt IS NOT NULL AND endDt IS NOT NULL) THEN
                  queryString = queryString || ' start_dt >= ''' || startDt || ''' AND ' || ' end_dt <= ''' || endDt || '''';
                  criteriaCount := criteriaCount+1;
              ELSIF (startDt IS NOT NULL) THEN
                  queryString = queryString || ' start_dt >= ''' || startDt || '''';
                  criteriaCount := criteriaCount+1;
              ELSIF (endDt IS NOT NULL) THEN
                  queryString = queryString || ' end_dt <= ''' || endDt || '''';
                  criteriaCount := criteriaCount+1;
              END IF;
          END IF;

      END IF;  -- criteria

      RAISE INFO 'Query String = %', queryString;

      RETURN QUERY EXECUTE queryString;
    END;

    $BODY$
  LANGUAGE plpgsql VOLATILE SECURITY DEFINER
  COST 100
  ROWS 1000;

ALTER FUNCTION operations.sf_get_j2_ops_for_search(text, bigint, bigint, bigint, date, date)
    OWNER TO pegasus;

GRANT EXECUTE ON FUNCTION operations.sf_get_j2_ops_for_search(text, bigint, bigint, bigint, 
    date, date) TO public;
GRANT EXECUTE ON FUNCTION operations.sf_get_j2_ops_for_search(text, bigint, bigint, bigint, 
    date, date) TO pegasus;
GRANT EXECUTE ON FUNCTION operations.sf_get_j2_ops_for_search(text, bigint, bigint, bigint, 
    date, date) TO read_write_operations;
GRANT EXECUTE ON FUNCTION operations.sf_get_j2_ops_for_search(text, bigint, bigint, bigint, 
    date, date) TO operations_user;

COMMENT ON FUNCTION operations.sf_get_j2_ops_for_search(text, bigint, bigint, bigint, date, 
    date) IS 'This stored procedure takes in search parameters and returns associated J2 Ops records.';