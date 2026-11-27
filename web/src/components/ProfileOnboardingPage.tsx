import React, { useState } from 'react';
import {
  VOLUNTEER_TYPE_LABELS,
  type DistrictItem,
  type EmployerOrganization,
  type UserProfile,
  type VolunteerStats,
  type VolunteerType,
} from '../types/maatram';

interface ProfileOnboardingPageProps {
  uid: string;
  email: string;
  defaultName: string;
  existingProfile: UserProfile | null;
  existingStats: VolunteerStats | null;
  districts: DistrictItem[];
  organizations: EmployerOrganization[];
  isBusy: boolean;
  onSaveProfile: (profile: UserProfile, isNew: boolean, availableForPv: boolean) => void;
}

export const ProfileOnboardingPage: React.FC<ProfileOnboardingPageProps> = ({
  uid,
  email,
  defaultName,
  existingProfile,
  existingStats,
  districts,
  organizations,
  isBusy,
  onSaveProfile,
}) => {
  const isNew = !existingProfile;
  const districtNames =
    districts.length > 0
      ? districts.map((d) => d.name).sort()
      : [
          'Tiruchirappalli',
          'Chennai',
          'Coimbatore',
          'Madurai',
          'Salem',
          'Thanjavur',
          'Erode',
          'Vellore',
        ];
  const orgList =
    organizations.length > 0
      ? organizations.map((o) => ({ id: o.id, name: o.name }))
      : [
          { id: 'org_maatram_stu', name: 'Maatram Student Chapter' },
          { id: 'org_zoho', name: 'Zoho Outreach Volunteers' },
          { id: 'org_tcs', name: 'TCS Maitree Alumni Wing' },
          { id: 'org_tvs', name: 'TVS Motor CSR Network' },
        ];

  const [fullName, setFullName] = useState(existingProfile?.fullName || defaultName);
  const [phone, setPhone] = useState(existingProfile?.phone || '');
  const [volunteerCode, setVolunteerCode] = useState(
    existingProfile?.volunteerCode || `MF-2026-${uid.slice(0, 4).toUpperCase()}`
  );
  const [volunteerType, setVolunteerType] = useState<VolunteerType>(
    (existingProfile?.volunteerType as VolunteerType) || 'STUDENT'
  );
  const [college, setCollege] = useState(existingProfile?.college || '');
  const [department, setDepartment] = useState(existingProfile?.department || '');
  const [yearOfStudy, setYearOfStudy] = useState(existingProfile?.yearOfStudy || '3rd Year');
  const [selectedDistrict, setSelectedDistrict] = useState(
    existingProfile?.district || districtNames[0]
  );
  const [city, setCity] = useState(existingProfile?.city || '');
  const [skills, setSkills] = useState(existingProfile?.skills || 'Mentoring, Communication');
  const [interests, setInterests] = useState(
    existingProfile?.interests || 'Physical Verification, Calling'
  );
  const [availability, setAvailability] = useState(existingProfile?.availability || 'Weekends');
  const [preferredContact, setPreferredContact] = useState(
    existingProfile?.preferredContact || 'WhatsApp'
  );
  const [selectedOrgId, setSelectedOrgId] = useState(
    existingProfile?.employerOrgId || orgList[0].id
  );
  const [selectedOrgName, setSelectedOrgName] = useState(
    existingProfile?.employerOrgName || orgList[0].name
  );
  const [consentData, setConsentData] = useState(existingProfile?.consentDataProcessing ?? true);
  const [consentLinkedIn, setConsentLinkedIn] = useState(
    existingProfile?.consentLinkedInPublic ?? false
  );
  const [availableForPv, setAvailableForPv] = useState(existingStats?.availableForPv ?? true);
  const [validationError, setValidationError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (fullName.trim().length < 2) {
      setValidationError('Please enter your full name (at least 2 characters).');
      return;
    }
    if (!consentData) {
      setValidationError('Data processing consent is required to register.');
      return;
    }
    setValidationError(null);
    onSaveProfile(
      {
        id: uid,
        userId: uid,
        fullName: fullName.trim(),
        email,
        phone: phone.trim(),
        volunteerCode: volunteerCode.trim(),
        volunteerType,
        college: college.trim(),
        department: department.trim(),
        yearOfStudy: yearOfStudy.trim(),
        district: selectedDistrict,
        city: city.trim(),
        skills: skills.trim(),
        interests: interests.trim(),
        availability: availability.trim(),
        preferredContact: preferredContact.trim(),
        employerOrgId: selectedOrgId,
        employerOrgName: selectedOrgName,
        consentDataProcessing: consentData,
        consentLinkedInPublic: consentLinkedIn,
        profileCompleted: true,
        role: existingProfile?.role || 'VOLUNTEER',
      },
      isNew,
      availableForPv
    );
  };

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div>
        <h2 className="text-xl font-bold text-maatram-navy">
          {isNew ? 'Volunteer Registration & Profile Setup' : 'My Volunteer Profile'}
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          Your contact details remain private and visible only to you and authorized Maatram Office
          coordinators.
        </p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="space-y-4 rounded-2xl border border-maatram-outline bg-white p-6 shadow-sm"
      >
        <div>
          <label className="block text-xs font-semibold text-maatram-navy">Full Name *</label>
          <input
            type="text"
            data-testid="profile_name_input"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black outline-none focus:border-maatram-blue"
            required
          />
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">Volunteer ID</label>
            <input
              type="text"
              value={volunteerCode}
              disabled={!isNew}
              onChange={(e) => isNew && setVolunteerCode(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline bg-slate-50 p-2.5 text-sm text-black disabled:opacity-70"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">Phone Number</label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black outline-none focus:border-maatram-blue"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-maatram-navy">
            Volunteer Category
          </label>
          <div className="mt-2 flex flex-wrap gap-2">
            {(Object.keys(VOLUNTEER_TYPE_LABELS) as VolunteerType[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setVolunteerType(t)}
                className={`rounded-xl border px-3.5 py-1.5 text-xs font-semibold ${
                  volunteerType === t
                    ? 'border-maatram-blue bg-maatram-blue text-white'
                    : 'border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft'
                }`}
              >
                {VOLUNTEER_TYPE_LABELS[t]}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-maatram-navy">
            College / Institution
          </label>
          <input
            type="text"
            value={college}
            onChange={(e) => setCollege(e.target.value)}
            className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black outline-none focus:border-maatram-blue"
          />
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">Department</label>
            <input
              type="text"
              value={department}
              onChange={(e) => setDepartment(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">Year / Alumni</label>
            <input
              type="text"
              value={yearOfStudy}
              onChange={(e) => setYearOfStudy(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-maatram-navy">
            Primary District (for Physical Verification Allocation)
          </label>
          <div className="mt-2 flex flex-wrap gap-2">
            {districtNames.map((dist) => (
              <button
                key={dist}
                type="button"
                onClick={() => setSelectedDistrict(dist)}
                className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                  selectedDistrict === dist
                    ? 'border-maatram-blue bg-maatram-blue text-white'
                    : 'border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft'
                }`}
              >
                {dist}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-maatram-navy">City / Town</label>
          <input
            type="text"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-maatram-navy">
            Employer / Chapter Organization
          </label>
          <div className="mt-2 flex flex-wrap gap-2">
            {orgList.map((org) => (
              <button
                key={org.id}
                type="button"
                onClick={() => {
                  setSelectedOrgId(org.id);
                  setSelectedOrgName(org.name);
                }}
                className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                  selectedOrgId === org.id
                    ? 'border-maatram-blue bg-maatram-blue text-white'
                    : 'border-maatram-outline bg-white text-maatram-navy hover:bg-maatram-blueSoft'
                }`}
              >
                {org.name}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">Skills</label>
            <input
              type="text"
              value={skills}
              onChange={(e) => setSkills(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">
              Areas of Interest
            </label>
            <input
              type="text"
              value={interests}
              onChange={(e) => setInterests(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">
              General Availability
            </label>
            <input
              type="text"
              value={availability}
              onChange={(e) => setAvailability(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-maatram-navy">
              Preferred Contact
            </label>
            <input
              type="text"
              value={preferredContact}
              onChange={(e) => setPreferredContact(e.target.value)}
              className="mt-1 w-full rounded-xl border border-maatram-outline p-2.5 text-sm text-black"
            />
          </div>
        </div>

        <div className="flex items-center justify-between gap-4 rounded-xl border border-maatram-outline bg-maatram-surface p-3.5">
          <div>
            <p className="text-sm font-bold text-maatram-navy">
              Available for District Physical Verification
            </p>
            <p className="text-xs text-slate-500">
              Turn off if temporarily unavailable (never penalized as inactive)
            </p>
          </div>
          <label className="relative inline-flex cursor-pointer items-center">
            <input
              type="checkbox"
              checked={availableForPv}
              onChange={(e) => setAvailableForPv(e.target.checked)}
              className="peer sr-only"
            />
            <div className="peer h-6 w-11 rounded-full bg-slate-300 after:absolute after:left-1 after:top-1 after:h-4 after:w-4 after:rounded-full after:bg-white after:transition-all peer-checked:bg-maatram-blue peer-checked:after:translate-x-5" />
          </label>
        </div>

        <div className="space-y-2 text-sm text-maatram-navy">
          <label className="flex items-start gap-2.5">
            <input
              type="checkbox"
              checked={consentData}
              onChange={(e) => setConsentData(e.target.checked)}
              className="mt-1 h-4 w-4 accent-maatram-blue"
            />
            <span>
              I consent to Maatram Foundation processing my volunteer records for opportunity
              coordination.
            </span>
          </label>
          <label className="flex items-start gap-2.5">
            <input
              type="checkbox"
              checked={consentLinkedIn}
              onChange={(e) => setConsentLinkedIn(e.target.checked)}
              className="mt-1 h-4 w-4 accent-maatram-blue"
            />
            <span>Consent to include my name in generated LinkedIn appreciation posts.</span>
          </label>
        </div>

        {validationError && (
          <p className="rounded-xl bg-maatram-redSoft p-3 text-xs font-semibold text-maatram-red">
            {validationError}
          </p>
        )}

        <button
          type="submit"
          disabled={isBusy}
          data-testid="save_profile_button"
          className="h-12 w-full rounded-xl bg-maatram-blue text-sm font-bold text-white shadow-sm transition hover:bg-maatram-blueDark disabled:opacity-50"
        >
          {isNew ? 'Complete Registration' : 'Save Profile Changes'}
        </button>
      </form>
    </div>
  );
};
