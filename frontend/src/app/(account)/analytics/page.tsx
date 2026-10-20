import { AnalyticsPage } from "../../../features/analytics/analytics-page";
export default async function Page({ searchParams }: PageProps<"/analytics">) {
  const { from, to } = await searchParams;
  return <AnalyticsPage rangeQuery={{ from, to }} />;
}
