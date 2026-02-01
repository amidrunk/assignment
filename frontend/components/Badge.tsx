type Tone = "emerald" | "sky" | "amber" | "slate";

type BadgeProps = {
  label: string;
  tone?: Tone;
};

const toneStyles: Record<Tone, { dot: string; chip: string }> = {
  emerald: {
    dot: "bg-emerald-400",
    chip: "border-emerald-100/15 bg-emerald-50/5 text-emerald-50",
  },
  sky: {
    dot: "bg-sky-300",
    chip: "border-sky-100/15 bg-sky-50/5 text-sky-50",
  },
  amber: {
    dot: "bg-amber-300",
    chip: "border-amber-100/20 bg-amber-50/5 text-amber-50",
  },
  slate: {
    dot: "bg-slate-200",
    chip: "border-white/10 bg-white/5 text-slate-100",
  },
};

function Badge({ label, tone = "slate" }: BadgeProps) {
  const style = toneStyles[tone];

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-semibold backdrop-blur-sm ${style.chip}`}
    >
      <span className={`h-2 w-2 rounded-full ${style.dot}`} />
      {label}
    </span>
  );
}

export default Badge;
