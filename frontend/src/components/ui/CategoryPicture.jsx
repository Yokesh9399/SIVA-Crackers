import { useState } from 'react';
import ProductArt from '../product/ProductArt.jsx';

/** Category picture uploaded by the owner, or the built-in illustration when there is none or it fails to load. */
export default function CategoryPicture({ category, className = '' }) {
  const [failed, setFailed] = useState(false);
  if (category.imageUrl && !failed) {
    return <img src={category.imageUrl} alt={category.name} loading="lazy" onError={() => setFailed(true)} className={`object-cover ${className}`} />;
  }
  return <ProductArt categorySlug={category.slug} label={category.name} className={className} />;
}
