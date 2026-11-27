import React, { useState } from 'react';
import { Eye, EyeOff, Lock, Mail } from 'lucide-react';
import type { UserRole } from '../types/maatram';
import { isFirebaseConfigured } from '../services/firebase';

type LoginRoleOption = 'STUDENT' | 'OFFICE_TEAM' | 'ADMIN';

const ROLE_META: Record<
  LoginRoleOption,
  { label: string; userRole: UserRole; sampleEmail: string; samplePassword: string; hint: string }
> = {
  STUDENT: {
    label: 'Student',
    userRole: 'VOLUNTEER',
    sampleEmail: 'student@maatram.org',
    samplePassword: 'student123',
    hint: 'Demo Student Login: student@maatram.org / student123 (or enter your email)',
  },
  OFFICE_TEAM: {
    label: 'Office Team',
    userRole: 'OFFICE_TEAM',
    sampleEmail: 'office@maatram.org',
    samplePassword: 'office123',
    hint: 'Office Team Shared Login: office@maatram.org / office123',
  },
  ADMIN: {
    label: 'Admin',
    userRole: 'ADMIN',
    sampleEmail: 'nrajasri99@gmail.com',
    samplePassword: 'admin123',
    hint: 'Admin Login: nrajasri99@gmail.com / admin123',
  },
};

interface AuthPageProps {
  onEmailPasswordSignIn: (name: string, email: string, district: string, role: UserRole) => void;
  onGoogleSignIn: () => Promise<void>;
}

export const AuthPage: React.FC<AuthPageProps> = ({
  onEmailPasswordSignIn,
  onGoogleSignIn,
}) => {
  const [selectedRole, setSelectedRole] = useState<LoginRoleOption>('STUDENT');
  const [emailInput, setEmailInput] = useState(ROLE_META.STUDENT.sampleEmail);
  const [passwordInput, setPasswordInput] = useState(ROLE_META.STUDENT.samplePassword);
  const [showPassword, setShowPassword] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleRoleSwitch = (role: LoginRoleOption) => {
    setSelectedRole(role);
    setEmailInput(ROLE_META[role].sampleEmail);
    setPasswordInput(ROLE_META[role].samplePassword);
    setErrorMessage(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedEmail = emailInput.trim();
    const pass = passwordInput;
    const meta = ROLE_META[selectedRole];

    if (selectedRole === 'STUDENT') {
      if (!trimmedEmail.includes('@') || !trimmedEmail.includes('.')) {
        setErrorMessage('Please enter a valid student Email ID.');
        return;
      }
      if (pass.length < 4) {
        setErrorMessage('Password must be at least 4 characters.');
        return;
      }
      const rawName =
        trimmedEmail
          .split('@')[0]
          .replace(/[._]/g, ' ')
          .trim()
          .split(/\s+/)
          .filter(Boolean)
          .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
          .join(' ') || 'Maatram Student';
      onEmailPasswordSignIn(rawName, trimmedEmail, 'Tiruchirappalli', 'VOLUNTEER');
      return;
    }

    if (selectedRole === 'OFFICE_TEAM') {
      if (
        trimmedEmail.toLowerCase() !== meta.sampleEmail.toLowerCase() ||
        pass !== meta.samplePassword
      ) {
        setErrorMessage(
          `Invalid Office Team credentials. Use ${meta.sampleEmail} and ${meta.samplePassword}.`
        );
        return;
      }
      onEmailPasswordSignIn('Maatram Office Team', meta.sampleEmail, 'Tiruchirappalli', 'OFFICE_TEAM');
      return;
    }

    if (
      trimmedEmail.toLowerCase() !== meta.sampleEmail.toLowerCase() ||
      pass !== meta.samplePassword
    ) {
      setErrorMessage(
        `Invalid Admin credentials. Use ${meta.sampleEmail} and ${meta.samplePassword}.`
      );
      return;
    }
    onEmailPasswordSignIn('Maatram Admin', meta.sampleEmail, 'Chennai', 'ADMIN');
  };

  const handleGoogleClick = async () => {
    setErrorMessage(null);
    setIsGoogleLoading(true);
    try {
      await onGoogleSignIn();
    } catch (err) {
      setErrorMessage(
        err instanceof Error
          ? err.message
          : 'Could not complete Google Sign-In. Please use Email ID and Password above.'
      );
    } finally {
      setIsGoogleLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-maatram-blueSoft via-white to-white px-4 py-8">
      <div className="w-full max-w-md space-y-5">
        <div className="flex flex-col items-center text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-maatram-blue text-xl font-bold text-white shadow-md">
            MC
          </div>
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-maatram-navy">
            MAATRAM CONNECT
          </h1>
          <p className="mt-1 text-sm text-slate-500">Simple Sign In</p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="space-y-4 rounded-3xl border border-maatram-outline bg-white p-6 shadow-sm"
        >
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-maatram-navy">
              Select Role
            </p>
            <div className="mt-2 flex flex-wrap gap-2">
              {(['STUDENT', 'OFFICE_TEAM', 'ADMIN'] as LoginRoleOption[]).map((role) => {
                const active = selectedRole === role;
                return (
                  <button
                    key={role}
                    type="button"
                    data-testid={`login_role_${role.toLowerCase()}`}
                    onClick={() => handleRoleSwitch(role)}
                    className={`rounded-xl border px-3.5 py-2 text-sm transition ${
                      active
                        ? 'border-maatram-blue bg-maatram-blueContainer font-bold text-maatram-navy'
                        : 'border-maatram-outline bg-white font-medium text-slate-600 hover:bg-maatram-blueSoft'
                    }`}
                  >
                    {ROLE_META[role].label}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="login-email" className="block text-xs font-semibold text-maatram-navy">
              Email ID
            </label>
            <div className="relative">
              <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-maatram-blue" />
              <input
                id="login-email"
                data-testid="login_email_input"
                type="email"
                value={emailInput}
                onChange={(e) => {
                  setEmailInput(e.target.value);
                  setErrorMessage(null);
                }}
                placeholder={ROLE_META[selectedRole].sampleEmail}
                className="w-full rounded-xl border border-maatram-outline bg-white py-3 pl-10 pr-3 text-sm font-medium text-black outline-none transition focus:border-maatram-blue focus:ring-2 focus:ring-maatram-blue/20"
                required
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="login-password" className="block text-xs font-semibold text-maatram-navy">
              Password
            </label>
            <div className="relative">
              <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-maatram-blue" />
              <input
                id="login-password"
                data-testid="login_password_input"
                type={showPassword ? 'text' : 'password'}
                value={passwordInput}
                onChange={(e) => {
                  setPasswordInput(e.target.value);
                  setErrorMessage(null);
                }}
                placeholder="Enter password"
                className="w-full rounded-xl border border-maatram-outline bg-white py-3 pl-10 pr-10 text-sm font-medium text-black outline-none transition focus:border-maatram-blue focus:ring-2 focus:ring-maatram-blue/20"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-slate-500 hover:bg-maatram-blueSoft hover:text-maatram-navy"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>

          <div className="rounded-xl bg-maatram-blueSoft px-3 py-2 text-xs font-medium text-maatram-navy">
            {ROLE_META[selectedRole].hint}
          </div>

          {errorMessage && (
            <div className="rounded-xl bg-maatram-redSoft px-3 py-2.5 text-xs font-semibold text-maatram-red">
              {errorMessage}
            </div>
          )}

          <button
            type="submit"
            data-testid="login_submit_button"
            className="h-12 w-full rounded-xl bg-maatram-blue text-sm font-bold text-white shadow-sm transition hover:bg-maatram-blueDark"
          >
            Sign In
          </button>

          <div className="flex items-center gap-3">
            <div className="h-px flex-1 bg-maatram-outline" />
            <span className="text-xs font-medium text-slate-400">or</span>
            <div className="h-px flex-1 bg-maatram-outline" />
          </div>

          <button
            type="button"
            data-testid="google_sign_in_button"
            disabled={isGoogleLoading}
            onClick={handleGoogleClick}
            className="flex h-12 w-full items-center justify-center gap-2 rounded-xl border-2 border-maatram-blue bg-white text-sm font-semibold text-maatram-blue transition hover:bg-maatram-blueSoft disabled:opacity-60"
          >
            {isGoogleLoading ? 'Signing in...' : 'Sign in with Google'}
          </button>

          {!isFirebaseConfigured && (
            <p className="text-center text-[11px] text-slate-400">
              Demo credentials work immediately. Configure Firebase keys in <code>.env</code> for live Google Sign-In & Cloud Firestore sync.
            </p>
          )}
        </form>
      </div>
    </div>
  );
};
