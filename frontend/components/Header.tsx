import { ReactNode } from "react";

type HeaderProps = {
  children: ReactNode;
  size?: "xl" | "lg" | "md";
  align?: "left" | "center";
  className?: string;
};

const sizeMap: Record<NonNullable<HeaderProps["size"]>, string> = {
  xl: "text-4xl sm:text-5xl",
  lg: "text-3xl sm:text-4xl",
  md: "text-2xl sm:text-3xl",
};

function Header({
  children,
  size = "xl",
  align = "left",
  className = "",
}: HeaderProps) {
  const alignClass = align === "center" ? "text-center" : "text-left";

  return (
    <h1
      className={`font-semibold leading-tight text-white ${sizeMap[size]} ${alignClass} ${className}`}
    >
      {children}
    </h1>
  );
}

export default Header;
