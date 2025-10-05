import { Stack, IconButton, Tooltip, Divider } from '@mui/material';
import FormatBoldIcon from '@mui/icons-material/FormatBold';
import FormatItalicIcon from '@mui/icons-material/FormatItalic';
import CodeIcon from '@mui/icons-material/Code';
import LinkIcon from '@mui/icons-material/Link';
import FormatListBulletedIcon from '@mui/icons-material/FormatListBulleted';
import FormatQuoteIcon from '@mui/icons-material/FormatQuote';
import HorizontalRuleIcon from '@mui/icons-material/HorizontalRule';
import LooksOneIcon from '@mui/icons-material/LooksOne';
import LooksTwoIcon from '@mui/icons-material/LooksTwo';
import Looks3Icon from '@mui/icons-material/Looks3';

import { useCallback } from 'react';

interface Props {
  value: string;
  onChange: (next: string) => void;
  textareaRef: React.RefObject<HTMLTextAreaElement>;
}

/**
 * Minimal Markdown toolbar that modifies the bound textarea's selection.
 * No external editor dependency.
 */
export default function MarkdownToolbar({ value, onChange, textareaRef }: Props) {
  const withSelection = useCallback(
    (fn: (text: string, start: number, end: number) => { next: string; selStart?: number; selEnd?: number }) => {
      const el = textareaRef.current;
      if (!el) return;
      const start = el.selectionStart ?? 0;
      const end   = el.selectionEnd   ?? 0;
      const { next, selStart, selEnd } = fn(value, start, end);
      onChange(next);
      // restore selection for better UX
      queueMicrotask(() => {
        el.focus();
        if (selStart != null && selEnd != null) {
          el.setSelectionRange(selStart, selEnd);
        } else {
          const pos = start + 2; // generic best-effort
          el.setSelectionRange(pos, pos);
        }
      });
    },
    [value, onChange, textareaRef],
  );

  const surround = (prefix: string, suffix = prefix, placeholder = 'text') =>
    withSelection((text, start, end) => {
      const sel = text.slice(start, end) || placeholder;
      const before = text.slice(0, start);
      const after  = text.slice(end);
      const inserted = `${prefix}${sel}${suffix}`;
      const next = `${before}${inserted}${after}`;
      const selStart = before.length + prefix.length;
      const selEnd   = selStart + sel.length;
      return { next, selStart, selEnd };
    });

  const prefixEachLine = (prefix: string) =>
    withSelection((text, start, end) => {
      const before = text.slice(0, start);
      const sel    = text.slice(start, end);
      const after  = text.slice(end);

      const block = (sel || '').length
        ? sel.split('\n').map(l => (l.length ? `${prefix}${l}` : l)).join('\n')
        : `${prefix}`;

      const next = `${before}${block}${after}`;
      const selStart = before.length + prefix.length;
      const selEnd   = selStart + (sel || '').length;
      return { next, selStart, selEnd };
    });

  const heading = (level: 1 | 2 | 3) =>
    withSelection((text, start, end) => {
      const before = text.slice(0, start);
      const sel    = text.slice(start, end) || 'Heading';
      const after  = text.slice(end);

      const lines = sel.split('\n').map(line => {
        const stripped = line.replace(/^#{1,6}\s*/, ''); // remove existing heading
        return `${'#'.repeat(level)} ${stripped}`;
      });
      const block = lines.join('\n');
      const next = `${before}${block}${after}`;
      const selStart = before.length + (level + 1); // after "### "
      const selEnd   = selStart + lines.join('\n').length - (level + 1);
      return { next, selStart, selEnd };
    });

  const insertHr = () =>
    withSelection((text, start, end) => {
      const before = text.slice(0, start);
      const after  = text.slice(end);
      const insertion = `\n\n---\n\n`;
      const pos = before.length + insertion.length;
      return { next: `${before}${insertion}${after}`, selStart: pos, selEnd: pos };
    });

  const insertLink = () =>
    withSelection((text, start, end) => {
      const sel = text.slice(start, end) || 'link text';
      const before = text.slice(0, start);
      const after  = text.slice(end);
      const url = window.prompt('Enter URL', 'https://') || 'https://';
      const inserted = `[${sel}](${url})`;
      const next = `${before}${inserted}${after}`;
      const selStart = before.length + 1;
      const selEnd   = selStart + sel.length;
      return { next, selStart, selEnd };
    });

  return (
    <Stack direction="row" spacing={0.5} alignItems="center" sx={{ mb: 1, flexWrap: 'wrap' }}>
      <Tooltip title="Bold (Ctrl+B)">
        <IconButton size="small" onClick={() => surround('**')}>
          <FormatBoldIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Italic (Ctrl+I)">
        <IconButton size="small" onClick={() => surround('_')}>
          <FormatItalicIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Inline code">
        <IconButton size="small" onClick={() => surround('`')}>
          <CodeIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Link">
        <IconButton size="small" onClick={insertLink}>
          <LinkIcon fontSize="small" />
        </IconButton>
      </Tooltip>

      <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />

      <Tooltip title="Heading 1">
        <IconButton size="small" onClick={() => heading(1)}>
          <LooksOneIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Heading 2">
        <IconButton size="small" onClick={() => heading(2)}>
          <LooksTwoIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Heading 3">
        <IconButton size="small" onClick={() => heading(3)}>
          <Looks3Icon fontSize="small" />
        </IconButton>
      </Tooltip>

      <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />

      <Tooltip title="Bullet list">
        <IconButton size="small" onClick={() => prefixEachLine('- ')}>
          <FormatListBulletedIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Quote">
        <IconButton size="small" onClick={() => prefixEachLine('> ')}>
          <FormatQuoteIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Horizontal rule">
        <IconButton size="small" onClick={insertHr}>
          <HorizontalRuleIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </Stack>
  );
}
