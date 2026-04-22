"""
HAQMS Synthetic Dataset Generator — v2
Produces: haqms_seed.sql

Queue model: one queue per PROVIDER per day (queues.provider_id).
Schema source: haqms_schema.sql — all column names matched exactly.

Run:  python generate_seed.py
Load: mysql -u root -p haqms < haqms_seed.sql
"""

import random
from datetime import date, timedelta, datetime
from faker import Faker

fake = Faker()
random.seed(42)
Faker.seed(42)

# ── configuration ─────────────────────────────────────────────────────────────
NUM_PROVIDERS    = 100
NUM_PATIENTS     = 10_000
NUM_APPOINTMENTS = 50_000
SCHEDULE_DAYS    = 60       # working days of schedule per provider
OUTPUT_FILE      = "haqms_seed.sql"

# ── Ghana data ────────────────────────────────────────────────────────────────
GH_FIRSTNAMES = [
    "Kwame","Kofi","Kwesi","Yaw","Kojo","Ama","Akosua","Abena",
    "Adwoa","Afua","Kwabena","Kwaku","Fiifi","Nana","Akua","Esi",
    "Efua","Araba","Maame","Yaa","Obeng","Serwaa","Asantewaa","Nyarko",
    "Boateng","Mensah","Darko","Frimpong","Antwi","Gyamfi","Prempeh",
    "Asante","Ofori","Agyemang","Adjei","Poku","Adusei","Owusu","Osei",
]
GH_LASTNAMES = [
    "Mensah","Asante","Boateng","Owusu","Osei","Frimpong","Antwi",
    "Darko","Amoah","Appiah","Acheampong","Ofori","Amponsah","Agyei",
    "Adjei","Quaye","Acquah","Sarpong","Barimah","Bonsu","Oti",
    "Tetteh","Lartey","Nkrumah","Baffour","Aidoo","Gyasi","Adomako",
    "Opoku","Attah","Bediako","Dankwah","Ennin","Fofie",
]
GH_DISTRICTS = [
    "Accra","Kumasi","Tamale","Takoradi","Cape Coast",
    "Sunyani","Koforidua","Ho","Bolgatanga","Wa","Tema",
    "Obuasi","Techiman","Keta","Berekum",
]
SPECIALISATIONS = [
    "General Practitioner","Cardiologist","Paediatrician",
    "Gynaecologist","Orthopaedic Surgeon","Dermatologist",
    "Neurologist","Ophthalmologist","ENT Specialist","Psychiatrist",
]
DEPARTMENTS = [
    "General Outpatient","Cardiology","Paediatrics",
    "Obstetrics and Gynaecology","Orthopaedics","Dermatology",
    "Neurology","Ophthalmology","ENT","Psychiatry",
]
REASONS = [
    "Routine check-up","Fever and headache","Persistent cough",
    "Back pain","Abdominal pain","Follow-up consultation",
    "Skin rash","High blood pressure review","Diabetes management",
    "Eye examination","Ear infection","Chest pain evaluation",
    "Antenatal visit","Vaccination","Wound dressing",
    "Malaria treatment","Typhoid follow-up","Blood test review",
    "Respiratory infection","Joint pain assessment",
    "Hypertension monitoring","Post-operative review",
    "Chronic pain management","Dental referral","Nutritional assessment",
]

# Priority distribution: 5% EMERGENCY, 15% URGENT, 80% REGULAR
PRIORITIES = (["EMERGENCY"] * 5 + ["URGENT"] * 15 + ["REGULAR"] * 80)

# BCrypt hash of "Test@1234"
BCR = "$2a$12$Nh23rkesYb2pcUgEgqqO/Ompf./Dq3p7TEUkTMGoizBgHVLA3kLO2"

# ── helpers ───────────────────────────────────────────────────────────────────
def esc(s):
    """Escape a string value for SQL insertion."""
    return str(s).replace("\\", "\\\\").replace("'", "\\'")

def gh_phone():
    """Realistic Ghanaian mobile number (+233XXXXXXXXX)."""
    prefixes = ["024","025","026","027","028","054","055","056","057","059"]
    return f"+233{random.choice(prefixes)[1:]}{random.randint(1000000, 9999999)}"

def working_dates(n):
    """Return n consecutive Mon–Fri working dates starting from 30 days ago."""
    dates = []
    d = date.today() - timedelta(days=30)
    while len(dates) < n:
        if d.weekday() < 5:
            dates.append(d)
        d += timedelta(days=1)
    return dates

def pick_status(days_offset):
    """Assign a realistic appointment status based on temporal distance from today."""
    if days_offset < -7:
        return random.choices(
            ["COMPLETED", "NO_SHOW", "CANCELLED"],
            weights=[80, 10, 10]
        )[0]
    elif days_offset < 0:
        return random.choices(
            ["COMPLETED", "NO_SHOW", "CANCELLED", "CONFIRMED"],
            weights=[60, 10, 10, 20]
        )[0]
    elif days_offset == 0:
        return random.choices(
            ["SCHEDULED", "CONFIRMED"],
            weights=[40, 60]
        )[0]
    else:
        return "SCHEDULED"

def pick_entry_status(appt_status):
    """
    Derive a queue_entry status from the appointment status.
    Only CONFIRMED and COMPLETED appointments have queue entries in this dataset.
    """
    if appt_status == "COMPLETED":
        return random.choices(
            ["COMPLETED", "MISSED"],
            weights=[90, 10]
        )[0]
    elif appt_status == "CONFIRMED":
        return random.choices(
            ["WAITING", "CALLED", "SERVING"],
            weights=[60, 20, 20]
        )[0]
    return None   # SCHEDULED, CANCELLED, NO_SHOW — no queue entry

# ── emit buffer ───────────────────────────────────────────────────────────────
lines = []

def emit(s):
    lines.append(s)

def flush_insert(table, columns, rows):
    """Emit a batched INSERT for a list of value-tuple strings."""
    if not rows:
        return
    emit(f"INSERT INTO {table}")
    emit(f"  ({columns})")
    emit("VALUES")
    emit("  " + ",\n  ".join(rows) + ";")

# ═════════════════════════════════════════════════════════════════════════════
print("Generating seed data...")

# ── Header ────────────────────────────────────────────────────────────────────
emit("-- HAQMS Synthetic Performance Dataset v2")
emit(f"-- {NUM_PATIENTS:,} patients | {NUM_PROVIDERS} providers | {NUM_APPOINTMENTS:,} appointments")
emit("-- Priority: 5% EMERGENCY, 15% URGENT, 80% REGULAR")
emit("-- Queue model: one queue per PROVIDER per day")
emit("-- Generated with fixed seed 42 — reproducible on every run")
emit("")
emit("SET FOREIGN_KEY_CHECKS = 0;")
emit("SET AUTOCOMMIT = 0;")
emit("SET UNIQUE_CHECKS = 0;")
emit("")

# ── Truncate (clean slate) ─────────────────────────────────────────────────────
emit("-- ── Truncate existing data ──────────────────────────────────────")
for tbl in [
    "audit_logs", "queue_entries", "queues", "appointments",
    "provider_schedules", "system_users", "healthcare_providers",
    "patients", "departments", "roles",
]:
    emit(f"TRUNCATE TABLE {tbl};")
emit("")

# ── Roles ─────────────────────────────────────────────────────────────────────
# Schema already seeds roles via INSERT in the DDL, but we truncated,
# so re-insert them here.
emit("-- ── Roles ───────────────────────────────────────────────────────")
emit("INSERT INTO roles (role_id, role_name, description) VALUES")
emit("  (1,'ADMIN',   'Full system administration access'),")
emit("  (2,'PROVIDER','Healthcare provider -- view/manage own appointments, manage queues'),")
emit("  (3,'PATIENT', 'Patient -- book and view own appointments');")
emit("")

# ── Departments ───────────────────────────────────────────────────────────────
emit("-- ── Departments ─────────────────────────────────────────────────")
dept_values = ",\n  ".join(
    f"({i+1},'{esc(name)}','Department of {esc(name)}')"
    for i, name in enumerate(DEPARTMENTS)
)
emit(f"INSERT INTO departments (department_id, name, description) VALUES\n  {dept_values};")
emit("")

# ── Healthcare providers ───────────────────────────────────────────────────────
emit("-- ── Healthcare providers ────────────────────────────────────────")
used_licences    = set()
provider_rows    = []
provider_dept_map = {}   # provider_id (1-based) → department_id

for pid in range(1, NUM_PROVIDERS + 1):
    dept_id = ((pid - 1) % 10) + 1   # evenly distributed: 10 per department
    spec    = SPECIALISATIONS[dept_id - 1]
    fn      = random.choice(GH_FIRSTNAMES)
    ln      = random.choice(GH_LASTNAMES)
    phone   = gh_phone()
    email   = f"dr.{fn.lower()}.{ln.lower()}{pid}@haqms.gh"

    while True:
        lic = f"MDC/RN/{random.randint(1000, 9999)}"
        if lic not in used_licences:
            used_licences.add(lic)
            break

    provider_rows.append(
        f"({pid},{dept_id},'{esc(fn)}','{esc(ln)}','{esc(spec)}',"
        f"'{esc(lic)}','{esc(phone)}','{esc(email)}',TRUE,NOW())"
    )
    provider_dept_map[pid] = dept_id

flush_insert(
    "healthcare_providers",
    "provider_id,department_id,first_name,last_name,specialisation,"
    "license_number,phone_number,email,is_active,created_at",
    provider_rows
)
emit("")

# ── Provider system users ─────────────────────────────────────────────────────
# Schema column order: user_id, role_id, username, password_hash, email,
#                      patient_id, provider_id, is_active, ...
emit("-- ── Provider system users ───────────────────────────────────────")
prov_user_rows = []
for pid in range(1, NUM_PROVIDERS + 1):
    uid   = pid                        # user_id 1–100 for providers
    uname = f"provider_{pid:03d}"
    email = f"dr.provider{pid}@haqms.gh"
    prov_user_rows.append(
        f"({uid},2,'{esc(uname)}','{esc(BCR)}','{esc(email)}',NULL,{pid},TRUE,NOW())"
    )
flush_insert(
    "system_users",
    "user_id,role_id,username,password_hash,email,patient_id,provider_id,is_active,created_at",
    prov_user_rows
)
emit("")

# ── Provider schedules ────────────────────────────────────────────────────────
emit("-- ── Provider schedules ──────────────────────────────────────────")
sched_id    = 1
sched_rows  = []
sched_index = {}   # (provider_id, date_str) → schedule_id

working = working_dates(SCHEDULE_DAYS)

for pid in range(1, NUM_PROVIDERS + 1):
    for d in working:
        d_str = d.isoformat()
        sched_rows.append(
            f"({sched_id},{pid},'{d_str}','08:00:00','17:00:00',50,TRUE,NOW())"
        )
        sched_index[(pid, d_str)] = sched_id
        sched_id += 1

    # flush every 1,000 rows
    if len(sched_rows) >= 1000:
        flush_insert(
            "provider_schedules",
            "schedule_id,provider_id,schedule_date,start_time,end_time,"
            "max_slots,is_available,created_at",
            sched_rows
        )
        sched_rows = []

if sched_rows:
    flush_insert(
        "provider_schedules",
        "schedule_id,provider_id,schedule_date,start_time,end_time,"
        "max_slots,is_available,created_at",
        sched_rows
    )
emit("")

# ── Patients ──────────────────────────────────────────────────────────────────
print("  Generating patients...")
emit("-- ── Patients ────────────────────────────────────────────────────")

used_phones = set()
GENDERS     = ["MALE", "FEMALE"]
BATCH       = 1000

for batch_start in range(0, NUM_PATIENTS, BATCH):
    patient_rows = []
    for pid in range(batch_start + 1, min(batch_start + BATCH, NUM_PATIENTS) + 1):
        fn       = random.choice(GH_FIRSTNAMES)
        ln       = random.choice(GH_LASTNAMES)
        gender   = random.choice(GENDERS)
        dob      = fake.date_of_birth(minimum_age=5, maximum_age=85).isoformat()
        district = random.choice(GH_DISTRICTS)

        while True:
            phone = gh_phone()
            if phone not in used_phones:
                used_phones.add(phone)
                break

        address    = f"{random.randint(1,999)} {fake.street_name()}, {esc(district)}"
        email      = f"{fn.lower()}{pid}@mail.gh"
        ghana_card = f"GHA-{random.randint(100000000, 999999999)}-{random.randint(0,9)}"

        # Schema column order: patient_id, ghana_card_number, first_name, last_name,
        #   date_of_birth, gender, phone_number, email, address, is_active
        patient_rows.append(
            f"({pid},'{ghana_card}','{esc(fn)}','{esc(ln)}','{gender}',"
            f"'{dob}','{phone}','{esc(email)}','{esc(address)}',TRUE,NOW())"
        )

    flush_insert(
        "patients",
        "patient_id,ghana_card_number,first_name,last_name,gender,"
        "date_of_birth,phone_number,email,address,is_active,created_at",
        patient_rows
    )

emit("")

# ── Patient system users ──────────────────────────────────────────────────────
print("  Generating patient accounts...")
emit("-- ── Patient system users ────────────────────────────────────────")

for batch_start in range(0, NUM_PATIENTS, BATCH):
    user_rows = []
    for pid in range(batch_start + 1, min(batch_start + BATCH, NUM_PATIENTS) + 1):
        uid   = NUM_PROVIDERS + pid   # user_id 101–10100
        uname = f"patient_{pid:05d}"
        email = f"patient{pid}@mail.gh"
        user_rows.append(
            f"({uid},3,'{esc(uname)}','{esc(BCR)}','{esc(email)}',{pid},NULL,TRUE,NOW())"
        )
    flush_insert(
        "system_users",
        "user_id,role_id,username,password_hash,email,patient_id,provider_id,is_active,created_at",
        user_rows
    )

emit("")

# ── Admin account ─────────────────────────────────────────────────────────────
emit("-- ── Admin account ───────────────────────────────────────────────")
admin_uid = NUM_PROVIDERS + NUM_PATIENTS + 1  # 10201
emit("INSERT INTO system_users")
emit("  (user_id,role_id,username,password_hash,email,patient_id,provider_id,is_active,created_at)")
emit("VALUES")
emit(f"  ({admin_uid},1,'admin','{BCR}','admin@haqms.gh',NULL,NULL,TRUE,NOW());")
emit("")

# ── Appointments ───────────────────────────────────────────────────────────────
# Schema columns (no appointment_time, no wait_minutes — those live in queue_entries):
#   appointment_id, patient_id, provider_id, department_id, schedule_id,
#   appointment_date, reason, appointment_priority, status,
#   booked_by_user_id, cancellation_reason
print("  Generating appointments...")
emit("-- ── Appointments ────────────────────────────────────────────────")

all_dates = [d.isoformat() for d in working]

# Track which appointments are CONFIRMED/COMPLETED so we can create queue entries
# Structure: {(provider_id, date_str): [(appt_id, patient_id, priority, status), ...]}
queue_candidates = {}

for batch_start in range(0, NUM_APPOINTMENTS, BATCH):
    appt_rows = []

    for appt_id in range(batch_start + 1,
                         min(batch_start + BATCH, NUM_APPOINTMENTS) + 1):
        patient_id  = random.randint(1, NUM_PATIENTS)
        provider_id = random.randint(1, NUM_PROVIDERS)
        priority    = random.choice(PRIORITIES)
        reason      = random.choice(REASONS)

        appt_date_str = random.choice(all_dates)
        appt_date_obj = date.fromisoformat(appt_date_str)
        days_offset   = (appt_date_obj - date.today()).days
        status        = pick_status(days_offset)

        # schedule_id lookup — guaranteed to exist for every (provider, date) pair
        sid     = sched_index.get((provider_id, appt_date_str))
        sid_sql = str(sid) if sid else "NULL"

        dept_id = provider_dept_map[provider_id]

        # cancellation_reason — NULL for non-cancelled, quoted string for CANCELLED
        if status == "CANCELLED":
            cancel_sql = f"'Patient cancelled in advance'"
        else:
            cancel_sql = "NULL"

        appt_rows.append(
            f"({appt_id},{patient_id},{provider_id},{dept_id},{sid_sql},"
            f"'{appt_date_str}','{esc(reason)}',"
            f"'{priority}','{status}',"
            f"NULL,{cancel_sql},NOW())"
        )

        # Collect appointments eligible for queue entry generation
        if status in ("CONFIRMED", "COMPLETED", "NO_SHOW"):
            key = (provider_id, appt_date_str)
            queue_candidates.setdefault(key, []).append(
                (appt_id, patient_id, priority, status)
            )

    flush_insert(
        "appointments",
        "appointment_id,patient_id,provider_id,department_id,schedule_id,"
        "appointment_date,reason,appointment_priority,status,"
        "booked_by_user_id,cancellation_reason,created_at",
        appt_rows
    )

    if batch_start % 10000 == 0:
        print(f"    Appointments: {batch_start + BATCH:,} / {NUM_APPOINTMENTS:,}")

emit("")

# ── Queues and Queue Entries ───────────────────────────────────────────────────
# One queue per PROVIDER per day.
# Only create queues for (provider, date) combinations that have
# at least one CONFIRMED, COMPLETED, or NO_SHOW appointment
# (those are the appointments that reached the check-in stage).
print("  Generating queues and queue entries...")
emit("-- ── Queues (one per provider per day) ──────────────────────────")

queue_id   = 1
entry_id   = 1
queue_rows = []
entry_rows = []

# Priority sort order for realistic queue position assignment
PRIORITY_ORDER = {"EMERGENCY": 0, "URGENT": 1, "REGULAR": 2}

for (provider_id, date_str), candidates in sorted(queue_candidates.items()):
    if not candidates:
        continue

    appt_date_obj = date.fromisoformat(date_str)
    days_offset   = (appt_date_obj - date.today()).days

    # Determine queue status based on date
    if days_offset < 0:
        q_status   = "CLOSED"
        opened_at  = f"'{date_str} 07:30:00'"
        closed_at  = f"'{date_str} 17:00:00'"
    elif days_offset == 0:
        q_status   = random.choice(["OPEN", "PAUSED"])
        opened_at  = f"'{date_str} 07:30:00'"
        closed_at  = "NULL"
    else:
        q_status  = "OPEN"
        opened_at = "NULL"
        closed_at = "NULL"

    total_reg   = len(candidates)
    current_pos = total_reg if days_offset < 0 else random.randint(0, total_reg)

    queue_rows.append(
        f"({queue_id},{provider_id},'{date_str}',"
        f"'{q_status}',{current_pos},{total_reg},"
        f"{opened_at},{closed_at},NOW())"
    )

    # Sort candidates by priority then random (simulates arrival order)
    sorted_candidates = sorted(
        candidates,
        key=lambda x: (PRIORITY_ORDER.get(x[2], 9), random.random())
    )

    for position, (appt_id, patient_id, priority, appt_status) in \
            enumerate(sorted_candidates, start=1):

        entry_status = pick_entry_status(appt_status)
        if entry_status is None:
            continue

        # Timestamps — realistic relative to queue date
        base_dt        = datetime.strptime(f"{date_str} 08:00:00", "%Y-%m-%d %H:%M:%S")
        checked_in_dt  = base_dt + timedelta(minutes=random.randint(0, 30) + (position - 1) * 8)
        checked_in_sql = f"'{checked_in_dt.strftime('%Y-%m-%d %H:%M:%S')}'"

        if entry_status in ("CALLED", "SERVING", "COMPLETED", "MISSED"):
            called_dt  = checked_in_dt + timedelta(minutes=random.randint(5, 45))
            called_sql = f"'{called_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
        else:
            called_sql = "NULL"

        if entry_status in ("SERVING", "COMPLETED"):
            serving_dt  = called_dt + timedelta(minutes=random.randint(1, 5))
            serving_sql = f"'{serving_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
        else:
            serving_sql = "NULL"

        if entry_status == "COMPLETED":
            completed_dt  = serving_dt + timedelta(minutes=random.randint(5, 30))
            completed_sql = f"'{completed_dt.strftime('%Y-%m-%d %H:%M:%S')}'"
            wait_mins     = int((serving_dt - checked_in_dt).total_seconds() // 60)
            wait_sql      = str(wait_mins)
        else:
            completed_sql = "NULL"
            wait_sql      = "NULL"

        if entry_status == "MISSED":
            completed_sql = "NULL"
            wait_sql      = "NULL"

        entry_rows.append(
            f"({entry_id},{queue_id},{appt_id},{patient_id},{position},"
            f"'{entry_status}',"
            f"{checked_in_sql},{called_sql},{serving_sql},{completed_sql},"
            f"{wait_sql},NOW())"
        )
        entry_id += 1

    queue_id += 1

    # Flush queues every 500 rows
    if len(queue_rows) >= 500:
        flush_insert(
            "queues",
            "queue_id,provider_id,queue_date,status,"
            "current_position,total_registered,"
            "opened_at,closed_at,created_at",
            queue_rows
        )
        queue_rows = []

    # Flush entries every 1,000 rows
    if len(entry_rows) >= 1000:
        flush_insert(
            "queue_entries",
            "entry_id,queue_id,appointment_id,patient_id,queue_position,"
            "status,checked_in_at,called_at,serving_started_at,completed_at,"
            "wait_minutes,created_at",
            entry_rows
        )
        entry_rows = []

# Final flush
if queue_rows:
    flush_insert(
        "queues",
        "queue_id,provider_id,queue_date,status,"
        "current_position,total_registered,"
        "opened_at,closed_at,created_at",
        queue_rows
    )
if entry_rows:
    flush_insert(
        "queue_entries",
        "entry_id,queue_id,appointment_id,patient_id,queue_position,"
        "status,checked_in_at,called_at,serving_started_at,completed_at,"
        "wait_minutes,created_at",
        entry_rows
    )

emit("")

# ── Footer ────────────────────────────────────────────────────────────────────
emit("COMMIT;")
emit("SET FOREIGN_KEY_CHECKS = 1;")
emit("SET UNIQUE_CHECKS = 1;")
emit("SET AUTOCOMMIT = 1;")
emit("")

# ── Verification queries ──────────────────────────────────────────────────────
emit("-- ── Verification ────────────────────────────────────────────────")
emit("SELECT 'patients'      AS tbl, COUNT(*) AS cnt FROM patients")
emit("UNION ALL")
emit("SELECT 'providers',             COUNT(*)        FROM healthcare_providers")
emit("UNION ALL")
emit("SELECT 'appointments',          COUNT(*)        FROM appointments")
emit("UNION ALL")
emit("SELECT 'queues',                COUNT(*)        FROM queues")
emit("UNION ALL")
emit("SELECT 'queue_entries',         COUNT(*)        FROM queue_entries;")
emit("")
emit("-- Priority distribution")
emit("SELECT appointment_priority, COUNT(*) AS cnt,")
emit("  ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM appointments), 1) AS pct")
emit("FROM appointments")
emit("GROUP BY appointment_priority")
emit("ORDER BY FIELD(appointment_priority,'EMERGENCY','URGENT','REGULAR');")
emit("")
emit("-- Appointments per department")
emit("SELECT d.name, COUNT(*) AS appointments")
emit("FROM appointments a")
emit("JOIN departments d ON d.department_id = a.department_id")
emit("GROUP BY d.name ORDER BY appointments DESC;")
emit("")
emit("-- Queue entries per status")
emit("SELECT status, COUNT(*) AS cnt FROM queue_entries GROUP BY status;")

# ── Write file ────────────────────────────────────────────────────────────────
print(f"\nWriting {OUTPUT_FILE}...")
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

size_mb = sum(len(l) for l in lines) / 1_048_576
total_queues  = queue_id - 1
total_entries = entry_id - 1

print(f"Done — {OUTPUT_FILE} ({size_mb:.1f} MB, {len(lines):,} lines)")
print(f"\nCounts generated:")
print(f"  Patients:      {NUM_PATIENTS:>8,}")
print(f"  Providers:     {NUM_PROVIDERS:>8,}")
print(f"  Appointments:  {NUM_APPOINTMENTS:>8,}")
print(f"  Queues:        {total_queues:>8,}")
print(f"  Queue entries: {total_entries:>8,}")
print(f"\nLoad with:")
print(f"  mysql -u root -p haqms < {OUTPUT_FILE}")
