import { InputHTMLAttributes } from "react";

type CheckboxProps = {
  label: string;
  helperText?: string;
} & InputHTMLAttributes<HTMLInputElement>;

function Checkbox({ label, helperText, className = "", ...rest }: CheckboxProps) {
  return (
    <label className="flex items-start gap-3 text-sm text-slate-200/90">
      <input
        type="checkbox"
        className={`peer mt-0.5 h-4 w-4 shrink-0 cursor-pointer rounded border border-white/25 bg-white/5 text-sky-300 outline-none transition focus:ring-2 focus:ring-sky-300/50 ${className}`}
        {...rest}
      />
      <span className="leading-5">
        <span className="font-semibold text-white">{label}</span>
        {helperText ? (
          <span className="block text-xs font-normal text-slate-300/80">{helperText}</span>
        ) : null}
      </span>
    </label>
  );
}

export default Checkbox;