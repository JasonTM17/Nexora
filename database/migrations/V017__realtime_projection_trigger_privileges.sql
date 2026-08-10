-- M3-DB01 forward repair: both private Realtime projections use UPSERT. The
-- runtime role intentionally has no SELECT on either relation, so their
-- trigger-only synchronizers run through the reviewed migrator-owned definer
-- boundary rather than gaining a direct read grant.

BEGIN;
SET LOCAL ROLE nexora_migrator;

CREATE OR REPLACE FUNCTION nexora.bump_realtime_authorization_epoch()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF TG_OP = 'DELETE' THEN
    INSERT INTO nexora.realtime_authorization_epochs (
      subject_id,
      authorization_epoch,
      updated_at
    ) VALUES (
      OLD.subject_id,
      1,
      transaction_timestamp()
    ) ON CONFLICT (subject_id) DO UPDATE
    SET authorization_epoch = nexora.realtime_authorization_epochs.authorization_epoch + 1,
        updated_at = EXCLUDED.updated_at;
    RETURN OLD;
  END IF;

  INSERT INTO nexora.realtime_authorization_epochs (
    subject_id,
    authorization_epoch,
    updated_at
  ) VALUES (
    NEW.subject_id,
    1,
    transaction_timestamp()
  ) ON CONFLICT (subject_id) DO UPDATE
  SET authorization_epoch = nexora.realtime_authorization_epochs.authorization_epoch + 1,
      updated_at = EXCLUDED.updated_at;

  IF TG_OP = 'UPDATE' AND OLD.subject_id IS DISTINCT FROM NEW.subject_id THEN
    INSERT INTO nexora.realtime_authorization_epochs (
      subject_id,
      authorization_epoch,
      updated_at
    ) VALUES (
      OLD.subject_id,
      1,
      transaction_timestamp()
    ) ON CONFLICT (subject_id) DO UPDATE
    SET authorization_epoch = nexora.realtime_authorization_epochs.authorization_epoch + 1,
        updated_at = EXCLUDED.updated_at;
  END IF;

  RETURN NEW;
END
$function$;

CREATE OR REPLACE FUNCTION nexora.sync_realtime_presence_resource()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, nexora
AS $function$
BEGIN
  IF TG_OP = 'DELETE' THEN
    DELETE FROM nexora.realtime_presence_resources
    WHERE resource_id = OLD.id;
    RETURN OLD;
  END IF;

  INSERT INTO nexora.realtime_presence_resources (
    resource_id,
    organization_id,
    updated_at
  ) VALUES (
    NEW.id,
    NEW.organization_id,
    transaction_timestamp()
  ) ON CONFLICT (resource_id) DO UPDATE
  SET organization_id = EXCLUDED.organization_id,
      updated_at = EXCLUDED.updated_at;

  RETURN NEW;
END
$function$;

REVOKE ALL ON FUNCTION nexora.bump_realtime_authorization_epoch() FROM PUBLIC;
REVOKE ALL ON FUNCTION nexora.sync_realtime_presence_resource() FROM PUBLIC;

COMMIT;
