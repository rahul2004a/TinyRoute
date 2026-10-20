"use client";

import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { Analytics } from "./analytics-api";

const dimensions = [
  ["referrers", "Referrers"],
  ["devices", "Devices"],
  ["operatingSystems", "Operating systems"],
  ["browsers", "Browsers"],
  ["countries", "Countries"],
  ["cities", "Cities"],
] as const;
const number = new Intl.NumberFormat("en");
export function AnalyticsReport({ report }: { report: Analytics }) {
  return (
    <div className="analytics-report">
      <div className="analytics-totals">
        <section aria-labelledby="range-total-label">
          <h2 id="range-total-label">Redirects in range</h2>
          <p className="analytics-total">{number.format(report.totalClicks)}</p>
          <p className="analytics-muted">
            {report.range.from} — {report.range.to} · UTC
          </p>
        </section>
        <section aria-labelledby="lifetime-total-label">
          <h2 id="lifetime-total-label">Lifetime redirects</h2>
          <p className="analytics-total analytics-total-secondary">
            {number.format(report.lifetimeClicks)}
          </p>
          <p className="analytics-muted">
            Across {report.link ? "this link" : "your current links"}, all dates
          </p>
        </section>
      </div>
      <section className="analytics-trend" aria-labelledby="daily-trend-title">
        <div className="analytics-section-heading">
          <div>
            <h2 id="daily-trend-title">Daily activity</h2>
            <p className="analytics-muted">
              Successful redirects per day · UTC
            </p>
          </div>
          <span className="analytics-muted">
            {report.dailyClicks.length} days
          </span>
        </div>
        <p className="sr-only">
          {report.totalClicks} successful redirects over{" "}
          {report.dailyClicks.length} days. The data table contains every daily
          count.
        </p>
        {report.totalClicks === 0 ? (
          <p className="analytics-empty">No redirects in this date range.</p>
        ) : (
          <div
            className="analytics-chart"
            role="group"
            aria-label="Daily redirect trend. Use Show data table for all values."
          >
            <ResponsiveContainer
              width="100%"
              height="100%"
              minWidth={0}
              initialDimension={{ width: 600, height: 260 }}
            >
              <AreaChart
                data={report.dailyClicks}
                accessibilityLayer
                margin={{ top: 16, right: 16, left: 0, bottom: 8 }}
              >
                <CartesianGrid vertical={false} stroke="var(--settings-line)" />
                <XAxis
                  dataKey="date"
                  minTickGap={42}
                  tickFormatter={(value: string) => value.slice(5)}
                  stroke="var(--settings-muted)"
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  allowDecimals={false}
                  width={40}
                  stroke="var(--settings-muted)"
                  tickLine={false}
                  axisLine={false}
                />
                <Tooltip
                  contentStyle={{
                    background: "var(--canvas)",
                    color: "var(--ink)",
                    border: "1px solid var(--border)",
                    borderRadius: 8,
                  }}
                  labelStyle={{ color: "var(--ink)" }}
                  itemStyle={{ color: "var(--ink)" }}
                />
                <Area
                  name="Redirects"
                  type="linear"
                  dataKey="count"
                  stroke="var(--ink)"
                  fill="var(--ink)"
                  fillOpacity={0.08}
                  strokeWidth={2}
                  isAnimationActive={false}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
        <details className="analytics-data">
          <summary>Show data table</summary>
          <div
            className="analytics-table-scroll"
            role="region"
            aria-label="Daily counts table"
            tabIndex={0}
          >
            <table>
              <caption>Daily redirect counts (UTC)</caption>
              <thead>
                <tr>
                  <th scope="col">Date (UTC)</th>
                  <th scope="col">Redirects</th>
                </tr>
              </thead>
              <tbody>
                {report.dailyClicks.map((day) => (
                  <tr key={day.date}>
                    <th scope="row">{day.date}</th>
                    <td>{number.format(day.count)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </details>
      </section>
      <div className="analytics-breakdowns">
        {dimensions.map(([key, title]) => (
          <section
            className="analytics-breakdown"
            aria-labelledby={`breakdown-${key}`}
            key={key}
          >
            <h2 id={`breakdown-${key}`}>{title}</h2>
            <table>
              <caption className="sr-only">{title}</caption>
              <thead>
                <tr>
                  <th scope="col">
                    {title === "Referrers" ? "Source" : "Category"}
                  </th>
                  <th scope="col">Redirects</th>
                </tr>
              </thead>
              <tbody>
                {report.breakdowns[key].map((bucket) => (
                  <tr key={`${bucket.kind}:${bucket.label}`}>
                    <th scope="row">
                      <span>{bucket.label}</span>
                      <span
                        className="analytics-bar"
                        aria-hidden="true"
                        style={{
                          width: `${report.totalClicks ? (100 * bucket.count) / report.totalClicks : 0}%`,
                        }}
                      />
                    </th>
                    <td>{number.format(bucket.count)}</td>
                  </tr>
                ))}
                {report.breakdowns[key].length === 0 ? (
                  <tr>
                    <td colSpan={2} className="analytics-muted">
                      No activity in this range.
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </section>
        ))}
      </div>
      <p className="analytics-muted analytics-privacy">
        Anonymous counts only. No visitor IP addresses or identifiers are
        stored. Geography is approximate.
      </p>
    </div>
  );
}
