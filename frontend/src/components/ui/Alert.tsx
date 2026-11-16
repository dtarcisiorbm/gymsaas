interface AlertProps {
  kind?: "error" | "success" | "info";
  children: React.ReactNode;
}

const styles: Record<NonNullable<AlertProps["kind"]>, string> = {
  error: "border-red-500/30 bg-red-500/10 text-red-200",
  success: "border-emerald-500/30 bg-emerald-500/10 text-emerald-200",
  info: "border-sky-500/30 bg-sky-500/10 text-sky-200",
};

export function Alert({ kind = "error", children }: AlertProps) {
  return (
    <p role={kind === "error" ? "alert" : "status"} className={`rounded-xl border px-4 py-3 text-sm ${styles[kind]}`}>
      {children}
    </p>
  );
}
