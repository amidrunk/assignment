import { ReactNode } from "react";

type CardProps = {
  children: ReactNode;
  className?: string;
  padding?: "none" | "sm" | "md" | "lg";
  blur?: boolean;
  border?: boolean;
};

const paddingMap = {
  none: "",
  sm: "p-6",
  md: "p-8",
  lg: "p-10",
};

function Card({
  children,
  className = "",
  padding = "lg",
  blur = true,
  border = true,
}: CardProps) {
  const base =
    "relative rounded-2xl bg-white/5 text-white shadow-2xl " +
    (border ? "border border-white/10 " : "") +
    (blur ? "backdrop-blur-xl " : "");

  const paddingClass = paddingMap[padding];

  return (
    <section className={`${base} ${paddingClass} ${className}`}>
      <div className="absolute inset-x-6 top-0 h-px bg-gradient-to-r from-transparent via-white/40 to-transparent" />
      {children}
    </section>
  );
}

export default Card;
