import { AuthLayout } from "../../../features/auth/auth-layout";
import { PasswordResetRequestForm } from "../../../features/auth/password-reset-form";

export default function PasswordResetPage() {
  return (
    <AuthLayout>
      <PasswordResetRequestForm />
    </AuthLayout>
  );
}
