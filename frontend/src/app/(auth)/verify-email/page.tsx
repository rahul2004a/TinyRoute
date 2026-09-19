import { RegistrationForm } from "../../../features/auth/registration-form";

export default function VerifyEmailPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-12">
      <RegistrationForm initialStep="verify" />
    </main>
  );
}
