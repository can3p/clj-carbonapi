open Core.Std
open Async.Std
open Cohttp_async

let data_source = Uri.of_string "http://localhost:8080/render"

let make_response status body =
    let headers = Cohttp.Header.of_list [
        "Content-Type","application/json";
        (*"Content-Length", body |> String.length |> Int.to_string*)
    ] in
    let resp = Response.make ~status ~headers () in
    (resp, Body.of_string body)

let process_target target =
    let source = Uri.add_query_params' data_source [
        ("format", "json");
        ("target", target)
    ] in
    Client.get source
    >>= fun (resp, body) -> Body.to_string body
    >>= fun (body) ->
        let json = Yojson.Safe.from_string body in
        json |> Yojson.Safe.pretty_to_string ~std:false |> return
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

let () =
    Server.create (Tcp.on_port 8081) handler |> ignore

let () =
    never_returns (Scheduler.go ())
