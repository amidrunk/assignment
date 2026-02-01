import { InputHTMLAttributes } from "react";

type InputProps = {
  label: string;
  name: string;
  helperText?: string;
} & InputHTMLAttributes<HTMLInputElement>;

function Input({ label, name, helperText, className = "", ...rest }: InputProps) {
  return (
    <label className="flex flex-col gap-2 text-sm font-medium text-slate-100">
      <span>{label}</span>
      <input
        name={name}
        className={`rounded-xl border border-white/15 bg-white/5 px-4 py-3 text-base font-normal text-white shadow-[0_10px_40px_-20px_rgba(0,0,0,0.6)] outline-none transition focus:border-sky-300 focus:bg-white/10 focus:ring-2 focus:ring-sky-300/50 ${className}`}
        {...rest}
      />
      {helperText ? (
        <span className="text-xs font-normal text-slate-300/80">{helperText}</span>
      ) : null}
    </label>
  );
}

export default Input;