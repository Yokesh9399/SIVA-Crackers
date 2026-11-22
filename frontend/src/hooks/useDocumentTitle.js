import { useEffect } from 'react';

/** Sets the browser tab title: "<title> | SIVA Crackers". */
export function useDocumentTitle(title) {
  useEffect(() => {
    if (title) document.title = `${title} | SIVA Crackers`;
  }, [title]);
}
