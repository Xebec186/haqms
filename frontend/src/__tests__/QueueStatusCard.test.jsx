import { render, screen } from '@testing-library/react';
import QueueStatusCard from '../../components/features/QueueStatusCard';

describe('QueueStatusCard', () => {

  test('renders nothing visible when entry is null', () => {
    const { container } = render(<QueueStatusCard entry={null} estimatedWait={0} />);
    expect(container.querySelector('[role="status"]')).not.toBeInTheDocument();
  });

  test('renders queue position for WAITING entry', () => {
    render(
      <QueueStatusCard
        entry={{ queuePosition: 7, status: 'WAITING' }}
        estimatedWait={45}
      />
    );
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(screen.getByText(/45 min/i)).toBeInTheDocument();
    expect(screen.getByText(/waiting/i)).toBeInTheDocument();
  });

  test('shows urgent call-to-action alert when status is CALLED', () => {
    render(
      <QueueStatusCard
        entry={{ queuePosition: 3, status: 'CALLED' }}
        estimatedWait={0}
      />
    );
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent(/proceed to the consultation room/i);
  });

  test('does NOT show estimated wait when status is not WAITING', () => {
    render(
      <QueueStatusCard
        entry={{ queuePosition: 2, status: 'SERVING' }}
        estimatedWait={15}
      />
    );
    expect(screen.queryByText(/estimated wait/i)).not.toBeInTheDocument();
  });

  test('shows SERVING message when status is SERVING', () => {
    render(
      <QueueStatusCard
        entry={{ queuePosition: 1, status: 'SERVING' }}
        estimatedWait={0}
      />
    );
    expect(screen.getByText(/currently being attended/i)).toBeInTheDocument();
  });

  test('has aria-live polite region', () => {
    render(
      <QueueStatusCard
        entry={{ queuePosition: 5, status: 'WAITING' }}
        estimatedWait={0}
      />
    );
    const region = screen.getByRole('status');
    expect(region).toHaveAttribute('aria-live', 'polite');
    expect(region).toHaveAttribute('aria-atomic', 'true');
  });
});
