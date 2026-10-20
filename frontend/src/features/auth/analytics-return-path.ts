/** Only application analytics pages may override the normal sign-in destination. */
export function analyticsReturnPath(value: string | undefined): string {
  if (
    !value ||
    value.length > 512 ||
    !value.startsWith("/") ||
    value.startsWith("//")
  )
    return "/links";
  try {
    const url = new URL(value, "https://tinyroute.invalid");
    if (
      url.origin !== "https://tinyroute.invalid" ||
      !/^\/analytics(?:\/[0-9a-fA-F-]{36})?$/.test(url.pathname)
    )
      return "/links";
    return url.pathname + url.search;
  } catch {
    return "/links";
  }
}
