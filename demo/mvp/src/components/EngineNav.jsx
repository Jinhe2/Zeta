import { Link } from 'react-router-dom'
import { ENGINE_LIST } from '../engines/registry'

export default function EngineNav({ active }) {
  return (
    <nav className="nav-links">
      {ENGINE_LIST.map((engine) =>
        active === engine.id ? (
          <span key={engine.id} className={`nav-active nav-active--${engine.id}`}>
            {engine.label}
          </span>
        ) : (
          <Link key={engine.id} to={engine.path} className={`nav-link nav-link--${engine.id}`}>
            {engine.label}
          </Link>
        ),
      )}
    </nav>
  )
}
