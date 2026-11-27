import { initializeApp, getApps, type FirebaseApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  type Auth,
} from 'firebase/auth';
import { getFirestore, type Firestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || '',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || '',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '',
};

const databaseId =
  import.meta.env.VITE_FIREBASE_DATABASE_ID ||
  'ai-studio-android-maatramc-01f1e4ac-a0ae-491a-9d08-d155a95dc9d9';

export const isFirebaseConfigured = Boolean(
  firebaseConfig.apiKey &&
    firebaseConfig.projectId &&
    firebaseConfig.appId &&
    !firebaseConfig.apiKey.startsWith('your_')
);

let app: FirebaseApp | null = null;
let authInstance: Auth | null = null;
let dbInstance: Firestore | null = null;

if (isFirebaseConfigured) {
  app = getApps().length > 0 ? getApps()[0] : initializeApp(firebaseConfig);
  authInstance = getAuth(app);
  dbInstance =
    databaseId && databaseId !== '(default)'
      ? getFirestore(app, databaseId)
      : getFirestore(app);
}

export const firebaseAuth = authInstance;
export const firestoreDb = dbInstance;

const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

export async function signInWithGoogleWeb(): Promise<void> {
  if (!firebaseAuth) {
    throw new Error(
      'Firebase Web configuration is not set in .env. Add VITE_FIREBASE_* keys or sign in using the Email ID & Password demo mode above.'
    );
  }
  await signInWithPopup(firebaseAuth, googleProvider);
}

export async function signOutFirebaseWeb(): Promise<void> {
  if (firebaseAuth) {
    await signOut(firebaseAuth);
  }
}
