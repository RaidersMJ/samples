-- ---------------------------------------------------------------------
-- helios.watch_list_view
--
-- This view is used to show all the watch list data with 
-- all the proper associations
--
-- author: MJBoldin <raidersmj@yahoo.com>
-- ---------------------------------------------------------------------

-- View: helios.watch_list_view
DROP VIEW IF EXISTS helios.watch_list_view;

CREATE OR REPLACE VIEW helios.watch_list_view AS
 SELECT 
    wl.watch_list_id,
    wl.watch_list_name,
    wl.start_date,
    wl.end_date,
    -- retrieve related agencies
    ( SELECT jsonb_agg(json_build_object('id', agency.agency_id, 'name', agency.agency_long_desc::text) ORDER BY agency.agency_long_desc) AS agencies
           FROM helios.agency
          WHERE (agency.agency_id IN ( SELECT relationship.to_entity_id
                   FROM helios.relationship
                  WHERE relationship.from_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'WATCH_LIST'::text)) AND relationship.from_entity_id = wl.watch_list_id AND relationship.to_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'AGENCY'::text)) AND relationship.reltype_id = (( SELECT entity_relationship_type.ent_rel_type_id
                           FROM helios.entity_relationship_type
                          WHERE entity_relationship_type.relationship_name::text = 'Contains'::text AND entity_relationship_type.reciprocal::text = 'On list'::text)) AND relationship.deleteflag = 'N'::bpchar)) AND agency.watch_list_agency = true) AS agencies,
    -- retrieve related organizations
    ( SELECT jsonb_agg(json_build_object('id', t.id, 'name', t.name) ORDER BY t.name) AS target_orgs
           FROM target.organization t
          WHERE (t.id IN ( SELECT relationship.to_entity_id
                   FROM helios.relationship
                  WHERE relationship.from_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'WATCH_LIST'::text)) AND relationship.from_entity_id = wl.watch_list_id AND relationship.to_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'TARGET_ORGANIZATION'::text)) AND relationship.reltype_id = (( SELECT entity_relationship_type.ent_rel_type_id
                           FROM helios.entity_relationship_type
                          WHERE entity_relationship_type.relationship_name::text = 'Contains'::text AND entity_relationship_type.reciprocal::text = 'On list'::text)) AND relationship.deleteflag = 'N'::bpchar)) AND t.deleted = false) AS target_orgs,
    -- retrieve related persons
    ( SELECT jsonb_agg(json_build_object('id', t.target_id, 'name', pd.known_name) ORDER BY pd.known_name) AS target_pois
           FROM helios.target t
             LEFT JOIN helios.carrier_description cd ON t.target_id = cd.target_id
             LEFT JOIN helios.person_description pd ON cd.carrier_description_id = pd.carrier_description_id
          WHERE (t.target_id IN ( SELECT relationship.to_entity_id
                   FROM helios.relationship
                  WHERE relationship.from_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'WATCH_LIST'::text)) AND relationship.from_entity_id = wl.watch_list_id AND relationship.to_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'TARGET_POI'::text)) AND relationship.reltype_id = (( SELECT entity_relationship_type.ent_rel_type_id
                           FROM helios.entity_relationship_type
                          WHERE entity_relationship_type.relationship_name::text = 'Contains'::text AND entity_relationship_type.reciprocal::text = 'On list'::text)) AND relationship.deleteflag = 'N'::bpchar)) AND t.deleted::text <> 'Y'::text) AS target_pois,
    -- retrieve related vessels
    ( SELECT jsonb_agg(json_build_object('id', t.id, 'name', t.name) ORDER BY t.name) AS target_vsls
           FROM target.vessel t
          WHERE (t.id IN ( SELECT relationship.to_entity_id
                   FROM helios.relationship
                  WHERE relationship.from_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'WATCH_LIST'::text)) AND relationship.from_entity_id = wl.watch_list_id AND relationship.to_entity_type_id = (( SELECT entity.entityname_id
                           FROM helios.entity
                          WHERE entity.entityname::text = 'TARGET_VESSEL'::text)) AND relationship.reltype_id = (( SELECT entity_relationship_type.ent_rel_type_id
                           FROM helios.entity_relationship_type
                          WHERE entity_relationship_type.relationship_name::text = 'Contains'::text AND entity_relationship_type.reciprocal::text = 'On list'::text)) AND relationship.deleteflag = 'N'::bpchar)) AND t.deleted = false AND t.is_alpha = false) AS target_vsls,
    wl.deleted,
    wl.data_creator_id,
    wl.data_created_dt,
    wl.data_modified_id,
    wl.data_modified_dt
   FROM helios.watch_list wl
  ORDER BY wl.data_modified_dt DESC;

COMMENT ON VIEW helios.watch_list_view IS 
       'This view aggregates watch list data from various tables into a common view.';

ALTER TABLE helios.watch_list_view OWNER TO pegasus;
GRANT SELECT, REFERENCES ON TABLE helios.watch_list_view TO pass_user_ro;
GRANT INSERT, SELECT, UPDATE ON TABLE helios.watch_list_view TO read_write_helios;