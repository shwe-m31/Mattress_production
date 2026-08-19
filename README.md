<div align="center">

<!-- ANIMATED BANNER -->
<img src="https://capsule-render.vercel.app/api?type=venom&color=0:0f2027,50:203a43,100:2c5364&height=220&section=header&text=PepsMattress&fontSize=72&fontColor=00d4ff&fontAlignY=55&desc=Automated%20Production%20Data%20Display%20System&descSize=18&descAlignY=75&descColor=a0c4ff&animation=fadeIn" width="100%"/>


> **Transforming mattress manufacturing with real-time intelligence.**  
> *From manual LCD displays → to a fully automated, live factory dashboard.*

</div>

---

## 🎬 Demo

<div align="center">

https://github.com/user-attachments/assets/e0aac817-3eab-4170-966d-eec9e5eec85a

</div>

---

## 📌 Problem → Solution

<table>
<tr>
<td width="50%">

### ❌ Before — Manual Process

- Operators **manually count** each mattress
- Data entered into an **LCD display** by hand
- **Human errors** distort production reports
- Supervisors have **zero real-time visibility**
- Operator time wasted on **non-production tasks**

</td>
<td width="50%">

### ✅ After — Automated Workflow

- **Live dashboard** pushes data instantly
- **Zero manual entry** — zero reporting lag
- Accurate, **error-free production stats**
- Factory-wide **TV display** gives instant visibility
- Operators stay focused — **100% on production**

</td>
</tr>
</table>

---

## 🗺️ System Architecture

```
                        🏭 PRODUCTION FLOOR
                               │
               ┌───────────────┴────────────────┐
               │                                │
        🛏 Spring Line                   🌙 Hypnos Line
               │                                │
               └───────────────┬────────────────┘
                               │
                    📡 Automatic Data Collection
                               │
                    🧠 Processing Module
                  ┌────────────┴───────────────┐
                  │  Count │ Type │ Size │ Time │
                  └────────────┬───────────────┘
                               │
                    🌐 React Web Dashboard
                               │
                    💻 Supervisor PC
                     Dashboard
```

---

## ⚡ Key Features

| Feature | Description | Status |
|---|---|---|
| 🔴 **Live Production Feed** | Real-time unit completion log with timestamp | ✅ Live |
| 📊 **Hourly Analytics Graph** | Visual bar chart of Spring vs Hypnos per hour | ✅ Live |
| 📦 **Product Breakdown** | Categorized by Single / Double / Queen / King | ✅ Live |
| 🔁 **Auto Refresh** | Configurable 15 min / 30 min / 1 hr intervals | ✅ Live |
| 🔐 **Secure Login** | Email-protected supervisor access | ✅ Live |
| 📺 **TV Display Mode** | Optimized for large factory floor screens | ✅ Live |

---

## 📊 Dashboard Modules

<details>
<summary><b>🔐 1. Secure Login System</b></summary>

<br/>

Authorized supervisors authenticate via email and password before accessing any production data. Prevents unauthorized access to sensitive manufacturing information.

</details>

<details>
<summary><b>📋 2. Production Summary Cards</b></summary>

<br/>

At-a-glance KPI cards displayed at the top of every dashboard view:

- **Total Spring Mattresses** produced today
- **Total Hypnos Mattresses** produced today
- **Combined Total** across all lines
- **Current Production Status** indicator

</details>

<details>
<summary><b>📈 3. Hourly Production Graph</b></summary>

<br/>

Interactive bar chart built with **Chart.js / Recharts** showing:

- Spring production per hour
- Hypnos production per hour
- Peak hour identification
- Slow period detection

</details>

<details>
<summary><b>🛏 4. Size-Wise Product Breakdown</b></summary>

<br/>

| Size | Spring | Hypnos | Total |
|---|---|---|---|
| Single | — | — | — |
| Double | — | — | — |
| Queen | — | — | — |
| King | — | — | — |

</details>

<details>
<summary><b>🔴 5. Live Recent Production Feed</b></summary>

<br/>

```
🟢 HYPNOS Mattress — KING      ✅ Completed at 09:09 AM
🟢 SPRING Mattress — QUEEN     ✅ Completed at 09:08 AM
🟢 HYPNOS Mattress — DOUBLE    ✅ Completed at 09:06 AM
🟢 SPRING Mattress — SINGLE    ✅ Completed at 09:04 AM
```

</details>

<details>
<summary><b>🔁 6. Configurable Auto-Refresh</b></summary>

<br/>

Admins configure dashboard refresh frequency:
- Every **15 minutes**
- Every **30 minutes**
- Every **1 hour**

No manual intervention needed. Factory screens always show fresh data.

</details>

---

## 🛠️ Tech Stack

<div align="center">

| Layer | Technology |
|---|---|
| **Frontend** | React.js · HTML5 · CSS3 · JavaScript |
| **Backend** | SpringBoot · Java · MySQL · Sping Data JPA |
| **Visualization** | Chart.js |

</div>

---

## 🔄 Workflow — Step by Step

```
STEP 1 ── 🏭 Production Begins
          Workers manufacture Spring & Hypnos mattresses.
          Each completed unit triggers a production event.
             │
STEP 2 ── 📡 Automatic Data Collection
          [ Mattress Type ]  [ Size ]  [ Time ]  [ Quantity ]
             │
STEP 3 ── 🧠 Data Processing
          Application calculates:
          → Total units   → By type   → By size   → Hourly trends
             │
STEP 4 ── 🌐 Dashboard Updates
          Web dashboard refreshes automatically.
          No manual trigger needed.
             │
STEP 5 ── 📺 Factory Display
          Supervisors see live data on TV screens
          across the entire production floor.
```

---

## 📈 Benefits

```
 Eliminates Human Error ──── Automatic data capture removes manual mistakes
 Saves Operator Time ──────── No more manual LCD updates
 Real-Time Monitoring ─────── Live production data across the floor
 Better Decisions ─────────── Trend graphs surface bottlenecks fast
 Scales Easily ────────────── Extend to multiple lines and factories
```


<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0f2027,100:2c5364&height=100&section=footer&fontColor=00d4ff" width="100%"/>

</div>
