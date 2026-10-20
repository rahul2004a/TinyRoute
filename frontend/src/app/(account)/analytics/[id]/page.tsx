import { AnalyticsPage } from "../../../../features/analytics/analytics-page";
export default async function Page({
  params,
  searchParams,
}: PageProps<"/analytics/[id]">) {
  const { id } = await params;
  const { from, to } = await searchParams;
  return <AnalyticsPage id={id} rangeQuery={{ from, to }} />;
}
