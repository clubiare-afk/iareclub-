ALTER TABLE deposits ADD COLUMN return_origin VARCHAR(255);

UPDATE deposits
   SET return_origin = 'https://iareclub-production.up.railway.app'
 WHERE return_origin IS NULL;

ALTER TABLE deposits ALTER COLUMN return_origin SET NOT NULL;
