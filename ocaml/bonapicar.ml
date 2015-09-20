open Core.Std
open Async.Std
open Cohttp_async

type run_options = {
    mutable source: string;
};;

let opts = {
    source = "";
};;

let make_response status body =
    let headers = Cohttp.Header.of_list [
        "Content-Type","application/json";
        (*"Content-Length", body |> String.length |> Int.to_string*)
    ] in
    let resp = Response.make ~status ~headers () in
    (resp, Body.of_string body)

let process_target target =
    let data_source = Uri.of_string (opts.source ^ "/render") in
    let source = Uri.add_query_params' data_source [
        ("format", "json");
        ("target", target)
    ] in
    Client.get source
    >>= fun (resp, body) -> Body.to_string body
    >>= fun (body) ->
        let metrics = Render_json_j.answer_of_string body in
        metrics |> Render_json_j.string_of_answer |> return
    >>= fun (body) ->
        let status = Response.status resp in
        make_response status body |> return

let render_handler target =
    match target with
    | Some(target) -> process_target target
    | None -> Server.respond_with_string "No target"

let render_params req =
    let uri = req |> Request.uri in
    match Uri.get_query_param uri "target" with
    | Some(target) -> if target = "" then None else Some target
    | _ -> None

let route path req =
    match path with
    | "/test" -> Server.respond_with_string "test"
    | "/render" | "/render/" ->
            req |> render_params |> render_handler
    | _ -> Server.respond_with_string "Not Found"

let handler ~body:_ _addr req =
    printf "got request\n";
    let path = Request.uri req |> Uri.path in
    route path req

let run source port () =
    opts.source <- source;
    Server.create (Tcp.on_port port) handler
    >>= fun _ -> Deferred.never ()

let () =
    Command.async_basic
        ~summary:"Carbon api wannabe"
        Command.Spec.(
            empty
            +> flag "--source" (optional_with_default
                                "http://localhost:4000"
                                string)
               ~doc:" A host to get the data from (default http://localhost:4000)"
            +> flag "--port" (optional_with_default
                                8081
                                int)
               ~doc:" A port to start the server on (default 8081)"
        ) run
    |> Command.run
