import { AuthLayout } from "../../../features/auth/auth-layout";
import { RegistrationForm } from "../../../features/auth/registration-form";

export default function RegisterPage() {
  return (
    <AuthLayout>
      <RegistrationForm />
    </AuthLayout>
  );
}
