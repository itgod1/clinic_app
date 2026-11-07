# Clinic Agent source snapshot

This directory contains the AgentScope source used by the clinic application, including the clinic-specific agent, business tools, MCP server, and tests.

- Upstream: https://github.com/agentscope-ai/agentscope
- Source revision: b6518511 (local clinic integration, before the later upstream updates).
- License: Apache-2.0; see LICENSE.
- This is ordinary source in the clinic repository, not a Git submodule. The original sibling checkout remains independent.

Create a Python 3.11+ virtual environment in this directory and install the project with the service and Redis extras:

    python -m venv .venv
    .venv/Scripts/python -m pip install -e ".[service,storage-redis]"

See examples/agent_service/README.md for service configuration and examples/agent_service/clinic_agent/mcp_server.py for the read-only MCP entry point. Runtime credentials, virtual environments, dependency caches, workspaces, and logs are not included.
