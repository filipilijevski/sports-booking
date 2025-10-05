import { Stack, Chip } from '@mui/material';
import { type Category } from '../lib/categories';

interface Props {
  categories:  Category[];
  selectedId:  number | null;
  onChange:    (id: number | null) => void;
  /** when true, row layout with wrapping */
  horizontal?: boolean;
}

export default function CategoryChips({
  categories,
  selectedId,
  onChange,
  horizontal = false,
}: Props) {
  return (
    <Stack
      direction={horizontal ? 'row' : 'column'}
      spacing={1}
      flexWrap={horizontal ? 'wrap' : 'nowrap'}
    >
      <Chip
        label="All"
        clickable
        color={selectedId === null ? 'primary' : 'default'}
        onClick={() => onChange(null)}
      />
      {categories.map((c) => (
        <Chip
          key={c.id}
          label={c.name}
          clickable
          color={selectedId === c.id ? 'primary' : 'default'}
          onClick={() => onChange(c.id)}
          sx={horizontal ? { mb:1 } : undefined}
        />
      ))}
    </Stack>
  );
}
