import { AuthLayout } from "../../../features/auth/auth-layout";
import { LoginForm } from "../../../features/auth/login-form";

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { error, returnTo } = await searchParams;

  return (
    <AuthLayout>
      <LoginForm
        oauthFailed={error === "oauth_failed"}
        returnTo={typeof returnTo === "string" ? returnTo : undefined}
      />
    </AuthLayout>
  );
}
