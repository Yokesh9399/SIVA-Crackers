import { PLACEHOLDER_TEXT } from '../../utils/constants.js';

/**
 * Shows a real business detail, or a clearly marked "To be updated" label when
 * the owner has not provided it yet. We never invent business information.
 */
export default function PlaceholderValue({ value, label = PLACEHOLDER_TEXT, className = '' }) {
  if (value) return <span className={className}>{value}</span>;
  return (
    <span className={`italic opacity-70 ${className}`} title="This detail will be added by the shop owner">
      {label}
    </span>
  );
}
