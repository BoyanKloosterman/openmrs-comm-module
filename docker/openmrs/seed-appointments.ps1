# Demo-rooster: Super User (demo-provider) beschikbaar op elk uur, behalve als slot vol is door boeking.
# Gebruik: .\docker\openmrs\seed-appointments.ps1 [-DaysAhead 90] [-StartHour 8] [-EndHour 18]

param(
    [int]$DaysAhead = 90,
    [int]$StartHour = 8,
    [int]$EndHour = 18
)

$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not (Test-Path "$root\docker-compose.yml")) { $root = (Get-Location).Path }
Push-Location $root

$envFile = Join-Path $root ".env"
$password = "Admin123"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^OMRS_ADMIN_USER_PASSWORD=(.+)$') { $password = $Matches[1].Trim() }
    }
}

$base = "http://localhost:8080/openmrs/ws/rest/v1"
$cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:$password"))
$h = @{ Authorization = "Basic $cred"; "Content-Type" = "application/json" }

function Get-Or-Create($listUri, $createBody) {
    $list = (Invoke-RestMethod -Uri $listUri -Headers $h).results
    if ($list.Count -gt 0) { return $list[0] }
    $uri = $listUri -replace '\?.*', ''
    return Invoke-RestMethod -Uri $uri -Headers $h -Method Post -Body ($createBody | ConvertTo-Json)
}

Write-Host "Appointment Scheduling: doorlopende beschikbaarheid ($DaysAhead dagen, $StartHour`:00-$EndHour`:00 UTC)..."

docker compose exec -T postgres psql -U openmrs_user -d openmrs -q -c @"
UPDATE global_property SET property_value = 'en_US' WHERE property = 'default_locale';
UPDATE user_property SET property_value = 'en_US' WHERE user_id = 1 AND property = 'defaultLocale';
"@ | Out-Null

$null = Get-Or-Create "$base/visittype?v=default" @{ name = "Outpatient"; description = "Default visit" }
$type = Get-Or-Create "$base/appointmentscheduling/appointmenttype?v=default" @{
    name = "Consult"; description = "Demo consult"; duration = 60
}
$loc = (Invoke-RestMethod -Uri "$base/location?v=default" -Headers $h).results[0]

$providers = (Invoke-RestMethod -Uri "$base/provider?v=default" -Headers $h).results
if ($providers.Count -eq 0) {
    $session = Invoke-RestMethod -Uri "$base/session" -Headers $h
    $providers = @(Invoke-RestMethod -Uri "$base/provider" -Headers $h -Method Post -Body (@{
            person = $session.user.person.uuid; identifier = "demo-provider"
        } | ConvertTo-Json))
}
$prov = $providers[0]

$today = (Get-Date).Date
$from = $today.ToString("yyyy-MM-dd")
$to = $today.AddDays($DaysAhead).ToString("yyyy-MM-dd")
$lastStartHour = $EndHour - 1
if ($lastStartHour -lt $StartHour) { throw "EndHour moet groter zijn dan StartHour" }

# Bulk: blok uitbreiden + tijdsloten (module markeert volle slots bij boeking)
$sql = @"
DO `$`$
DECLARE
  v_block_id INTEGER;
  v_type_id INTEGER;
  v_creator INTEGER := 1;
  v_day DATE;
  v_hour INTEGER;
  v_start TIMESTAMP;
  v_end TIMESTAMP;
  v_added INTEGER := 0;
  v_range_start TIMESTAMP := CURRENT_DATE + make_interval(hours => $StartHour);
  v_range_end TIMESTAMP := (CURRENT_DATE + $DaysAhead) + make_interval(hours => $EndHour);
BEGIN
  SELECT appointment_type_id INTO v_type_id
  FROM appointmentscheduling_appointment_type WHERE name = 'Consult' AND retired = false LIMIT 1;

  SELECT ab.appointment_block_id INTO v_block_id
  FROM appointmentscheduling_appointment_block ab
  JOIN provider p ON ab.provider_id = p.provider_id
  WHERE p.identifier = 'demo-provider' AND ab.voided = false
  ORDER BY ab.appointment_block_id DESC
  LIMIT 1;

  IF v_block_id IS NULL THEN
    INSERT INTO appointmentscheduling_appointment_block (
      location_id, provider_id, start_date, end_date, uuid, creator, date_created, voided
    )
    SELECT l.location_id, p.provider_id, v_range_start, v_range_end,
           gen_random_uuid()::text, v_creator, NOW(), false
    FROM provider p, location l
    WHERE p.identifier = 'demo-provider' AND l.retired = false
    ORDER BY l.location_id LIMIT 1
    RETURNING appointment_block_id INTO v_block_id;
  ELSE
    UPDATE appointmentscheduling_appointment_block
    SET start_date = LEAST(start_date, v_range_start),
        end_date = GREATEST(end_date, v_range_end)
    WHERE appointment_block_id = v_block_id;
  END IF;

  IF v_type_id IS NOT NULL THEN
    INSERT INTO appointmentscheduling_block_type_map (appointment_type_id, appointment_block_id)
    SELECT v_type_id, v_block_id
    WHERE NOT EXISTS (
      SELECT 1 FROM appointmentscheduling_block_type_map
      WHERE appointment_type_id = v_type_id AND appointment_block_id = v_block_id
    );
  END IF;

  FOR v_day IN SELECT generate_series(CURRENT_DATE, CURRENT_DATE + $DaysAhead, '1 day')::date LOOP
    FOR v_hour IN $StartHour..$lastStartHour LOOP
      v_start := v_day + make_interval(hours => v_hour);
      v_end := v_start + interval '1 hour';
      IF NOT EXISTS (
        SELECT 1 FROM appointmentscheduling_time_slot ts
        WHERE ts.appointment_block_id = v_block_id
          AND ts.voided = false
          AND ts.start_date = v_start
      ) THEN
        INSERT INTO appointmentscheduling_time_slot (
          appointment_block_id, start_date, end_date, uuid, creator, date_created, voided
        ) VALUES (
          v_block_id, v_start, v_end, gen_random_uuid()::text, v_creator, NOW(), false
        );
        v_added := v_added + 1;
      END IF;
    END LOOP;
  END LOOP;

  RAISE NOTICE 'Nieuwe tijdsloten: %', v_added;
END `$`$;
"@

docker compose exec -T postgres psql -U openmrs_user -d openmrs -c $sql

$count = docker compose exec -T postgres psql -U openmrs_user -d openmrs -t -A -c @"
SELECT COUNT(*) FROM appointmentscheduling_time_slot ts
JOIN appointmentscheduling_appointment_block ab ON ts.appointment_block_id = ab.appointment_block_id
JOIN provider p ON ab.provider_id = p.provider_id
WHERE p.identifier = 'demo-provider' AND ts.voided = false
  AND ts.start_date >= CURRENT_DATE AND ts.start_date < CURRENT_DATE + $($DaysAhead + 1);
"@

Write-Host ""
Write-Host "Klaar. demo-provider heeft $count uur-slots in de komende $DaysAhead dagen ($StartHour`:00-$EndHour`:00 UTC)."
Write-Host "Geboekte afspraken vullen een slot; Find Available Times toont die tijd niet meer."
Write-Host "Formulier: Between = vandaag t/m +$DaysAhead dagen, overdag, Find Available Times."
Write-Host "Opnieuw inloggen als locale net op en_US is gezet."
Pop-Location
