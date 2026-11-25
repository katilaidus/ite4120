import { Route } from 'react-router-dom';
import { GuidelineList } from './GuidelineList';
import { ReviewReport } from './ReviewReport';

/**
 * This component's routes. App.tsx picks them up through its
 * `pages/*\/routes.tsx` glob, so the component plugs itself in without
 * touching shared code.
 */
export const routes = [
  <Route key="guidelines" path="/guidelines" element={<GuidelineList />} />,
  <Route key="guidelines-review" path="/guidelines/review" element={<ReviewReport />} />,
];
