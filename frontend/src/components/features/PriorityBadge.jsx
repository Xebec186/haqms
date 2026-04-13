import Badge from '../ui/Badge';

export default function PriorityBadge({ priority }) {
  return <Badge label={priority ?? 'REGULAR'} />;
}
