import { AuthLayout } from "../../../../features/auth/auth-layout";
import { PasswordResetConfirmForm } from "../../../../features/auth/password-reset-form";

export default function PasswordResetConfirmPage() {
  return (
    <AuthLayout>
      <PasswordResetConfirmForm />
    </AuthLayout>
  );
}
