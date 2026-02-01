import { ButtonHTMLAttributes, ReactNode } from "react";

type ButtonProps = {
  children: ReactNode;
  variant?: "primary" | "ghost";
  fullWidth?: boolean;
} & ButtonHTMLAttributes<HTMLButtonElement>;

function Button({
  children,
  variant = "primary",
  fullWidth = true,
  className = "",
  ...rest
}: ButtonProps) {
  const base =
    "inline-flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold transition focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-300";
  const variants = {
    primary:
      "bg-sky-400 text-slate-950 shadow-[0_12px_30px_-12px_rgba(56,189,248,0.7)] hover:bg-sky-300 active:translate-y-px",
    ghost:
      "border border-white/20 bg-white/5 text-white hover:border-white/40 hover:bg-white/10 active:translate-y-px",
  };

  return (
    <button
      className={`${base} ${variants[variant]} ${fullWidth ? "w-full" : ""} ${className}`}
      {...rest}
    >
      {children}
    </button>
  );
}

export default Button;