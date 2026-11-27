# MAATRAM CONNECT — Web Application

Responsive React + Vite + TypeScript web application for **Maatram Foundation**, backed by **Firebase Authentication** and **Cloud Firestore**.

## Features
- **Simple Blue & White Login** (`Student`, `Office Team`, `Admin`) with pre-filled demo credentials and **Google Sign-In** (`GoogleAuthProvider`).
- **Student / Volunteer Portal**: Student progress KPIs, **Volunteer Contribution Summary Chart** (Hours & Completed Tasks), **Volunteer Works** 4-category hub (*Physical Verification*, *Calling & BGV*, *Mock & Panel Interviews*, *Events & Coordination*), PV availability toggle, request-only slot acceptance, assigned work & case workspace, hour submission, badges, Student Leaderboard, LinkedIn appreciation post generator, and notifications.
- **Office Team Portal**: Availability Polling & Office self-vote (`Vote Available in Poll`), Works by Category, Request Approval/Waitlist/Rejection, **Fair PV Allocation Engine**, PV/Calling/Assignment verification, CSV import/export, and Hours verification.
- **Admin Portal**: Organization analytics charts, Volunteer engagement classification, Employer CSR reports & CSV export, District & Threshold settings, Role promotion, Audit logs, and Demo workspace seeding.

## Local Development
```bash
cd web
cp .env.example .env
# Fill VITE_FIREBASE_* values from your Firebase Console Web App config
npm install
npm run dev
```

## Deploy to Vercel
1. Push the repository to GitHub/GitLab/Bitbucket.
2. In Vercel, import the project and set **Root Directory** to `web`.
3. Framework Preset: **Vite** (`Build Command`: `npm run build`, `Output Directory`: `dist`).
4. Add all `VITE_FIREBASE_*` environment variables from `.env.example` in Vercel Project Settings -> Environment Variables.
5. Deploy, then add your Vercel domain (`*.vercel.app`) to **Firebase Console -> Authentication -> Settings -> Authorized domains**.
